#!/usr/bin/env python3

import sys
import os
import logging
from atlassian import Jira

BUILD_WEB_URL = os.getenv('BUILD_WEB_URL')
JOB_NAME = os.getenv('JOB_NAME')
TEST_FAILURE = os.getenv('TEST_FAILURE', 'false') == 'true'
JIRA_TOKEN = os.getenv('JIRA_TOKEN')

logging.basicConfig(format='%(levelname)s: %(message)s', level=logging.INFO)
logging.getLogger('JiraIssue').addHandler(logging.StreamHandler(sys.stdout))

jira = Jira(
    url='https://hyland.atlassian.net/',
    username='alfresco-build@hyland.com',
    password=JIRA_TOKEN
)

tests = {
        'arm64_health_check'                  : {'project_key': 'ACS', 'components': ['Repository']}
}


def get_summary_name():
    """Generate name of issue. """
    return f"{CURRENT_RELEASE} - ARM64 ACS Packaging CI failure"


def get_description():
    """Generate issue description. """
    return (
        f'ARM64 ACS Packaging CI failure\n'
        f'Build URL: {BUILD_WEB_URL}\n'
    )


def get_comment():
    """Generate comment. """
    return (
        f'The latest ARM64 ACS Packaging CI passed\n'
        f'Build URL: {BUILD_WEB_URL}\n'
    )


def get_labels():
    """Return list of labels to assign. """
    return []


def get_project_id(job_name):
    """Get project id based on type of tests. """
    try:
        return tests[job_name]['project_key']
    except KeyError:
        return None

def get_project_components(job_name):
    """Get project id based on type of tests. """
    try:
        return tests[job_name]['components']
    except KeyError:
        return None


def get_issue_key():
    """If issue with same name exist return key of it. If not return False"""

    project_id = get_project_id(JOB_NAME)
    if not project_id:
        return
    epic_id = get_epic_id_to_assign(project_id)
    if not epic_id:
        return
    sql_query = f'project = {project_id} AND "Release Train" = "{CURRENT_RELEASE}" AND issuetype = "bug"  AND  "parentEpic" = {epic_id}'
    result = jira.jql(sql_query)
    for issue in result['issues']:
        if issue['fields']['summary'] == get_summary_name():
            if issue['fields']['status']['name'].upper() in ['TO DO', 'IN PROGRESS', 'BACKLOG', 'OPEN', 'REVIEW', 'VERIFY']:
                logging.info("Issue already created")
                return issue['key']
    return False


def create_issue():
    """Create issue in Jira"""
    project_id = get_project_id(JOB_NAME)
    fields = {
        'project': {
            'key': project_id
        },
        'issuetype': {
            'name': 'Bug'
        },
        'summary': get_summary_name(),
        'description': get_description(),
        'labels': get_labels(),
        'versions': [{'name': 'none'}],
        # customfield_13654
        get_custom_field_id('Release Train'): {'value': CURRENT_RELEASE},
        # customfield_10100
        get_custom_field_id('Epic Link'): get_epic_id_to_assign(project_id),
        # customfield_13657
        get_custom_field_id('Bug Priority'): {'value': 'Category 2'},
    }
    if get_project_components(JOB_NAME):
        acs_fields = get_acs_component_field()
        fields.update(acs_fields)
    jira.issue_create(fields)

def get_acs_component_field():
    """Acs project require components field to be fill. """
    acs_fields = {'components': []}
    components = get_project_components(JOB_NAME)
    for component in components:
        acs_fields['components'].append({'name': component})
    return acs_fields

def get_epic_id_to_assign(project_id):
    """Return Epic id to assign a bug. """

    try:
        sql_query = f"""project = {project_id} AND
        'Release Train' = '{CURRENT_RELEASE}' AND
        issuetype = 'Epic' AND
        Summary ~ '{CURRENT_RELEASE} % Content Services Maintenance'
        ORDER BY issuekey"""
        result = jira.jql(sql_query)
        return result['issues'][0]['key']
    except IndexError:
        logging.error(f"Can't find the appropriate epic in {project_id}")
        return


def get_custom_field_id(field_name):
    """Return id of custorm field. """
    for custom_field in jira.get_all_custom_fields():
        if custom_field['name'] == field_name:
            return custom_field['key']
    return ''


def whether_create_issue():
    """Try to list issues in project to see if user have access to project. """

    if not JOB_NAME in tests:
        logging.error(
            f"Test name: '{JOB_NAME}' not found in config dict. Configure Jira project in this file if you want automatically created issues")
        sys.exit(0)
    try:
        # Check if build-user have access to project.
        project_id = get_project_id(JOB_NAME)
        if not project_id:
            return False
        sql_query = f"project = {project_id} AND issuetype = 'Epic'"
        jira.jql(sql_query)
    except BaseException:
        logging.error(
            "Could not list issues. Is user alfresco-build have access to this project?")
        return False

    # check if there is epic to assign in project
    epic_id = get_epic_id_to_assign(project_id)
    if not epic_id:
        return False

    return True


def set_current_release():
    """Set current release name. """
    sql_query = 'project = "OPSEXP" AND status IN ("Open","In Progress") ORDER BY created DESC'
    output = jira.jql(sql_query)
    epics = output['issues']
    release_trains = []

    for epic in epics:
        # if epic is linked to some release train
        if epic['fields']['customfield_13654']:
            release_trains.append(epic['fields']['customfield_13654']['value'])
    return max(set(release_trains), key=release_trains.count)


if __name__ == "__main__":
    CURRENT_RELEASE = set_current_release()

    if not TEST_FAILURE:
        logging.info("Test passed, close issue if there is one")
        # If tests passed and jira issue in 'TO DO', 'IN PROGRESS', 'BACKLOG', 'OPEN', 'REVIEW', 'VERIFY' state, close it
        key = get_issue_key()
        if key:
            logging.info(f"Closing issue {key}")
            jira.issue_add_comment(key, get_comment())
            jira.set_issue_status(key, "Done", fields={'fixVersions': [{'name': 'none'}]})

    else:
        logging.info("Test failed, create issue")
        # Checks before trying to create jira issue
        # 1. Is test declared in python dict
        # 2. Does build-user have access to project
        # 3. Is epic for Content Services maintenance created in project
        if whether_create_issue():
            if not get_issue_key():
                create_issue()

