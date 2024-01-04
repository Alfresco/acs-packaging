# This script creates HotFix branches for repo, share and packaging projects, and updates the master branches ready for the next SP/major release.
# To use this script you need to install gitpython and lxml:
# 'pip install gitpython'
# 'pip install lxml'

import argparse
import logging
import os
import re
import subprocess
import sys
from xml.etree import ElementTree as et

SERVICE_PACK = 'servicepack'
HOTFIX = 'hotfix'

POM_NS = 'http://maven.apache.org/POM/4.0.0'
COMMUNITY_PACKAGING = 'acs-community-packaging'
COMMUNITY_REPO = 'alfresco-community-repo'
ENTERPRISE_REPO = 'alfresco-enterprise-repo'
ENTERPRISE_SHARE = 'alfresco-enterprise-share'
ACS_PACKAGING = 'acs-packaging'
PROJECTS = [ACS_PACKAGING, ENTERPRISE_SHARE, ENTERPRISE_REPO, COMMUNITY_REPO, COMMUNITY_PACKAGING]
YAML_DICT = {ACS_PACKAGING: 'master_release.yml', COMMUNITY_PACKAGING: 'ci.yml'}

# read command line arguments
parser = argparse.ArgumentParser(description="Create git branches after ACS release.")
parser.add_argument('-r', '--release', metavar='x.y.z', help='release version (x.y.z format)')
parser.add_argument('-n', '--next_dev', metavar='x.y.z', help='next development version (x.y.z format)')
parser.add_argument('-v', '--verbose', action='store_true', help='print out verbose processing information')
parser.add_argument('-s', '--skip_push', action='store_true', help='skip git push')
parser.add_argument('-t', '--test_branches', action='store_true', help='use test branches')
parser.add_argument('-c', '--cleanup', action='store_true', help='cleanup test branches')
parser.add_argument('-a', '--ahead', action='store_true', help='create branches ahead of release')
parser.add_argument('-z', '--trace', action='store_true', help='trace processing information')

if len(sys.argv) == 1:
    parser.print_help()
    parser.exit()

# set global variables
script_path = os.path.dirname(sys.argv[0])
script_abspath = os.path.abspath(script_path)
root_dir_path_end = script_abspath.find(ACS_PACKAGING)
root_abspath = script_abspath[:root_dir_path_end]

args = parser.parse_args()

release_version = args.release if args.release else input("Enter released version (mandatory, use x.y or x.y.z format): ")
next_dev_version = args.next_dev if args.next_dev else input(
    "Enter next development version (optional, use x.y or x.y.z format or leave empty): ")

# setup logging
logger = logging.getLogger('CONSOLE')
console_handler = logging.StreamHandler()
if args.verbose:
    logger.setLevel(logging.DEBUG)
    console_handler.setLevel(logging.DEBUG)
else:
    logger.setLevel(logging.INFO)
    console_handler.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
console_handler.setFormatter(formatter)
logger.addHandler(console_handler)

logger.debug("Released version: %s" % release_version)
logger.debug("Next development version: %s" % next_dev_version)
logger.debug("Skip push: %s" % args.skip_push)
logger.debug("Use test branches: %s" % args.test_branches)
logger.debug("Cleanup test branches: %s" % args.cleanup)


def get_version_number(rel_ver, index):
    return int(rel_ver.split(".")[index])


def increment_version(rel_ver, branch_type):
    logger.debug("Incrementing %s version for %s branch" % (rel_ver, branch_type))
    if branch_type == HOTFIX:
        incremented = get_version_number(rel_ver, 2) + 1
        versions = rel_ver.split(".")
        versions[2] = str(incremented)
        return ".".join(versions)
    if branch_type == SERVICE_PACK:
        incremented = get_version_number(rel_ver, 1) + 1
        versions = rel_ver.split(".")
        versions[1] = str(incremented)
        versions[2] = "0"
        return ".".join(versions)


def get_next_dev_version(type):
    if next_dev_version:
        logger.debug("Getting next dev version from input parameter (%s)" % next_dev_version)
        return next_dev_version
    else:
        logger.debug("Getting next dev version. Incrementing released %s for %s" % (release_version, type))
        return increment_version(release_version, type)


def switch_dir(project):
    logger.debug("Current dir: %s" % os.getcwd())
    os.chdir(os.path.dirname(root_abspath))
    if project != 'root':
        os.chdir(project)
    logger.debug("Switched dir to: %s" % os.getcwd())


def get_xml_tag_value(xml_path, tag_path):
    xml_tree = load_xml(xml_path)
    return xml_tree.getroot().find(tag_path).text


def update_xml_tag(xml_tree, tag_path, new_value):
    xml_tag = xml_tree.getroot().find(tag_path)
    xml_tag.text = new_value
    return xml_tree


def load_xml(xml_path):
    logger.debug("Loading %s XML file" % xml_path)
    et.register_namespace("", POM_NS)
    xml_tree = et.ElementTree()
    xml_tree.parse(xml_path)
    return xml_tree


def update_acs_ver_pom_properties(project, version):
    prefix = "acs." if project == COMMUNITY_REPO else ""
    switch_dir(project)
    pom_path = "pom.xml"
    pom_tree = load_xml(pom_path)
    split_version = version.split(".")
    logger.debug("Setting ACS versions (%s) in %s pom.xml" % (split_version, project))
    update_xml_tag(pom_tree, "{%s}properties/{%s}%sversion.major" % (POM_NS, POM_NS, prefix), split_version[0])
    update_xml_tag(pom_tree, "{%s}properties/{%s}%sversion.minor" % (POM_NS, POM_NS, prefix), split_version[1])
    update_xml_tag(pom_tree, "{%s}properties/{%s}%sversion.revision" % (POM_NS, POM_NS, prefix), split_version[2])
    pom_tree.write(pom_path)
    switch_dir("root")


def update_scm_tag(tag, project):
    switch_dir(project)
    pom_path = "pom.xml"
    pom_tree = load_xml(pom_path)
    logger.debug("Setting scm tag to %s in %s pom.xml" % (tag, project))
    update_xml_tag(pom_tree, "{%s}scm/{%s}tag" % (POM_NS, POM_NS), tag)
    pom_tree.write(pom_path)
    switch_dir('root')


def update_line(text: list[str], text_to_match, replacement_value):
    regex = re.compile(text_to_match + ".*", re.IGNORECASE)
    for i in range(len(text)):
        if text_to_match in text[i]:
            line = i
    if line:
        text[line] = regex.sub("%s %s" % (text_to_match, replacement_value), text[line])

    return text


def update_ci_yaml(filename, project, rel_version, dev_version):
    ci_yaml_path = os.path.join(project, ".github", "workflows")
    switch_dir(ci_yaml_path)
    with open(filename, 'r') as file:
        text = file.readlines()

    release_version_match = "RELEASE_VERSION:"
    development_version_match = "DEVELOPMENT_VERSION:"

    if text:
        logger.debug("Setting RELEASE_VERSION, DEVELOPMENT_VERSION (%s, %s) in %s ci.yml" % (rel_version, dev_version, project))
        update_line(text, release_version_match, rel_version)
        update_line(text, development_version_match, dev_version)
        with open(filename, 'w') as file:
            file.writelines(text)

    switch_dir('root')


def read_property(text: list[str], key):
    for i in range(len(text)):
        if key in text[i]:
            line = i
    return text[line].split("=")[1] if line else None


def increment_schema(project, increment):
    properties_path = os.path.join(project, "repository", "src", "main", "resources", "alfresco")
    filename = "repository.properties"
    switch_dir(properties_path)
    with open(filename, 'r') as file:
        text = file.readlines()

    key = "version.schema="
    schema = read_property(text, key)
    new_schema = int(schema) + increment

    logger.debug("Updating property version.schema from %s to %s in %s" % (schema, new_schema, project))
    update_line(text, key, str(new_schema))

    with open(filename, 'w') as file:
        file.writelines(text)

    switch_dir('root')


def update_acs_comm_pck_dependencies(branch_type, project):
    switch_dir(project)
    xml_tree = load_xml("pom.xml")
    logger.debug("Updating comm-repo and comm-share dependencies in %s" % project)
    comm_repo_next_ver = increment_version(calculate_hotfix_version(COMMUNITY_REPO), branch_type)
    logger.debug("comm-repo dependency: %s" % comm_repo_next_ver)
    update_xml_tag(xml_tree, "{%s}properties/{%s}dependency.alfresco-community-repo.version" % (POM_NS, POM_NS), comm_repo_next_ver)
    update_xml_tag(xml_tree, "{%s}parent/{%s}version" % (POM_NS, POM_NS), comm_repo_next_ver)
    comm_share_next_ver = increment_version(calculate_hotfix_version(ENTERPRISE_SHARE), branch_type)
    logger.debug("comm-share dependency: %s" % comm_share_next_ver)
    update_xml_tag(xml_tree, "{%s}properties/{%s}dependency.alfresco-community-share.version" % (POM_NS, POM_NS), comm_share_next_ver)
    logger.debug("comm-repo dependency set to %s" % comm_repo_next_ver)
    switch_dir('root')


def set_ags_test_versions(project, version):
    properties_path = os.path.join(project, "amps", "ags", "rm-community", "rm-community-repo", "test", "resources", "alfresco")
    filename = "version.properties"
    switch_dir(properties_path)
    with open(filename, 'r') as file:
        text = file.readlines()

    logger.debug("Updating versions to %s in version.properties file in %s" % (version, project))
    major_key = "version.major="
    update_line(text, major_key, version.split[0])
    minor_key = "version.minor="
    update_line(text, minor_key, version.split[1])
    revision_key = "version.revision="
    update_line(text, revision_key, version.split[2])

    with open(filename, 'w') as file:
        file.writelines(text)

    switch_dir('root')


def calculate_increment(version, next_dev_ver):
    ver = version.split(".")
    next_ver = next_dev_ver.split(".")
    if ver[0] < next_ver[0]:
        return 1000
    if ver[1] < next_ver[1]:
        return 100
    return 1


def update_ent_repo_acs_label(project, version, branch_type):
    switch_dir(project)
    filename = "pom.xml"
    if branch_type == 'hotfix':
        xml_tree = load_xml(filename)
        update_xml_tag(xml_tree, "{%s}properties/{%s}acs.version.label" % (POM_NS, POM_NS), ".1")
    else:
        with open(filename, 'r') as file:
            text = file.readlines()
        update_line(text, "<acs.version.label", " /> <!-- %s.<acs.version.label> -->" % version)
        with open(filename, 'w') as file:
            file.writelines(text)
        switch_dir('root')


def exec_cmd(cmd_args):
    logger.debug("Executing command line of %s" % " ".join(cmd_args))
    try:
        ret = subprocess.run(cmd_args, shell=True) if args.trace else subprocess.run(cmd_args, shell=True, stdout=subprocess.DEVNULL)
        ret.check_returncode()
    except subprocess.CalledProcessError as e:
        logger.error("Error:\nreturn code: %s\nOutput: %s" % (e.returncode, e.stderr.decode("utf-8")))
        raise



def set_versions(project, version, profiles: list[str]):
    switch_dir(project)
    if "packaging" in project:
        snapshot_ver = version + "-SNAPSHOT"
    else:
        ver = version.split(".")
        if len(ver) == 4:
            ver.pop()
        snapshot_ver = ".".join(ver) + ".1-SNAPSHOT"

    arguments = ["mvn", "versions:set", "-DgenerateBackupPoms=false", "-DnewVersion=%s" % snapshot_ver, "-P%s" % ",".join(profiles)]
    logger.debug("Updating versions to %s in pom of %s" % (snapshot_ver, project))
    exec_cmd(arguments)
    switch_dir('root')


def checkout_branch(project, branch):
    switch_dir(project)
    logger.debug("Checking out %s branch in %s" % (branch, project))
    exec_cmd(["git", "fetch"])
    exec_cmd(["git", "checkout", branch])
    switch_dir('root')


def create_branch(project, branch, tag):
    switch_dir(project)
    checkout_branch(project, tag)
    switch_dir(project)
    logger.debug("Creating %s branch in %s from %s tag" % (branch, project, tag))
    exec_cmd(["git", "switch", "-c", branch])
    switch_dir('root')


def commit_and_push(project, message):
    logger.debug("Committing changes in %s. Commit message: %s" % (project, message))
    switch_dir(project)
    exec_cmd(["git", "commit", "--all", "-m", message])
    if not args.skip_push:
        logger.debug("Pushing changes in %s to remote." % project)
        exec_cmd(["git", "push"])
    switch_dir('root')


def calculate_hotfix_branch():
    rel_ver = release_version.split(".")
    rel_ver[2] = "N"
    prefix = "test/release/" if args.test_branches else "release/"
    hotfix_branch = prefix + ".".join(rel_ver)
    logger.debug("Calculated hotfix branch as %s " % hotfix_branch)
    return hotfix_branch


def calculate_hotfix_version(project):
    checkout_branch(ACS_PACKAGING, 'master' if args.ahead else release_version)
    switch_dir(ACS_PACKAGING)
    ent_repo_ver = get_xml_tag_value("pom.xml",
                                     "{%s}properties/{%s}dependency.alfresco-enterprise-repo.version" % (POM_NS, POM_NS))
    ent_share_ver = get_xml_tag_value("pom.xml",
                                      "{%s}properties/{%s}dependency.alfresco-enterprise-share.version" % (POM_NS, POM_NS))
    switch_dir('root')
    if project == ACS_PACKAGING or project == COMMUNITY_PACKAGING:
        return release_version
    elif project == ENTERPRISE_REPO:
        return ent_repo_ver
    elif project == ENTERPRISE_SHARE:
        return ent_share_ver
    elif project == COMMUNITY_REPO:
        checkout_branch(ENTERPRISE_REPO, ent_repo_ver)
        switch_dir(ENTERPRISE_REPO)
        comm_repo_ver = get_xml_tag_value("pom.xml",
                                          "{%s}properties/{%s}dependency.alfresco-community-repo.version" % (POM_NS, POM_NS))
        switch_dir('root')
        return comm_repo_ver


def update_project(project, version, branch_type):
    profiles = ["dev"] if "packaging" in project else ["ags"]
    set_versions(project, version, profiles)
    update_scm_tag('HEAD', project)
    next_dev_ver = get_next_dev_version(branch_type)
    if project == ACS_PACKAGING:
        update_ci_yaml(YAML_DICT.get(project), project, version, next_dev_ver) if branch_type == SERVICE_PACK else update_ci_yaml(
            YAML_DICT.get(project), project, version + "-A1", next_dev_ver + "-A1-SNAPSHOT")
    elif project == ENTERPRISE_SHARE:
        update_acs_ver_pom_properties(project, version)
    elif project == ENTERPRISE_REPO:
        update_ent_repo_acs_label(project, version, branch_type)
    elif project == COMMUNITY_REPO:
        update_acs_ver_pom_properties(project, version)
        increment_schema(project, calculate_increment(version, next_dev_ver))
        set_ags_test_versions(project, version)
    elif project == COMMUNITY_PACKAGING:
        update_ci_yaml(YAML_DICT.get(project), project, version, next_dev_ver)
        update_acs_comm_pck_dependencies(branch_type, project)


def log_progress(project, message):
    logger.info("---------------------------------------------")
    logger.info("Processing:")
    logger.info(project)
    logger.info("Message:")
    logger.info(message)
    logger.info("---------------------------------------------")


def create_hotfix_branches():
    hotfix_branch = calculate_hotfix_branch()
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        log_progress(project, "Creating hotfix branches")
        rel_tag_version = 'master' if args.ahead else calculate_hotfix_version(project)
        create_branch(project, hotfix_branch, rel_tag_version)
        version = increment_version(rel_tag_version, HOTFIX)
        update_project(project, version, HOTFIX)
        commit_and_push(project, "Creating hotfix branch %s for %s ACS release [skip ci]" % (hotfix_branch, release_version))


def modify_master_branches():
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        log_progress(project, "Updating master for next release")
        next_dev_ver = get_next_dev_version(SERVICE_PACK)
        checkout_branch(project, 'master')
        update_project(project, next_dev_ver, SERVICE_PACK)
        commit_and_push(project, "Updating master branch to %s after %s ACS release [skip ci]" % (next_dev_ver, release_version))


if release_version:
    create_hotfix_branches()
    modify_master_branches()

