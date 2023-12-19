# This script creates HotFix branches for repo, share and packaging projects, and updates the master branches ready for the next SP/major release.
# To use this script you need to install gitpython and lxml:
# 'pip install gitpython'
# 'pip install lxml'

import argparse
import sys
import os
import logging
from xml.etree import ElementTree as et
import re
import subprocess

POM_NS = "http://maven.apache.org/POM/4.0.0"
PROJECTS = ["acs-packaging", "enterprise-share", "enterprise-repo", "community-repo", "acs-community-packaging"]
YAML_DICT = {"acs-packaging": "master_release.yml", "acs-community-packaging": "ci.yml"}

# read command line arguments
parser = argparse.ArgumentParser(description='Create git branches after ACS release.')
parser.add_argument('-r', '--release', metavar='x.y.z', help='release version (x.y.z format)')
parser.add_argument('-n', '--next_dev', metavar='x.y.z', help='next development version (x.y.z format)')
parser.add_argument('-v', '--verbose', action='store_true', help='Print out verbose processing information')
parser.add_argument('-s', '--skip_push', action='store_true', help='skip git push')
parser.add_argument('-t', '--test_branches', action='store_true', help='use test branches')
parser.add_argument('-c', '--cleanup', action='store_true', help='cleanup test branches')

if len(sys.argv) == 1:
    parser.print_help()
    parser.exit()

# set global variables
script_path = os.path.dirname(sys.argv[0])
script_abspath = os.path.abspath(script_path)
root_dir_path_end = script_abspath.find("acs-packaging")
root_abspath = script_abspath[:root_dir_path_end]

args = parser.parse_args()

release_version = args.release if args.release else input("Enter released version (mandatory, use x.y or x.y.z format): ")
next_dev_version = args.next_dev if args.next_dev else input(
    "Enter next development version (optional, use x.y or x.y.z format or leave empty): ")

# setup logging
logger = logging.getLogger("CONSOLE")
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
    if branch_type == "hotfix":
        incremented = get_version_number(rel_ver, 2) + 1
        versions = rel_ver.split(".")
        versions[2] = string(incremented)
        return ".".join(versions)
    if branch_type == "servicepack":
        incremented = get_version_number(rel_ver, 1) + 1
        versions = rel_ver.split(".")
        versions[1] = string(incremented)
        versions[2] = "0"
        return ".".join(versions)


def get_next_dev_version(type):
    if next_dev_version is None:
        return increment_version(release_version, type)
    else:
        return next_dev_version


def switch_dir(project):
    logger.debug("Current dir: %s" % os.getcwd())
    os.chdir(os.path.dirname(root_abspath))
    if project != "root":
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


def update_acs_ver_pom_properties(version, project):
    if project == "alfresco-community-repo":
        prefix = "acs."
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
    switch_dir("root")


def update_ci_yaml(filename, project, release_version, next_dev_version):
    ci_yaml_path = os.path.join(project, ".github", "workflows")
    switch_dir(ci_yaml_path)
    rel_regex = re.compile("RELEASE_VERSION:.*", re.IGNORECASE)
    dev_regex = re.compile("DEVELOPMENT_VERSION:.*", re.IGNORECASE)
    with open(filename, "r") as file:
        text = file.readlines()

    for i in range(len(text)):
        if "RELEASE_VERSION:" in text[i]:
            rel_line = i
        if "DEVELOPMENT_VERSION:" in text[i]:
            dev_line = i

    logger.debug("Setting RELEASE_VERSION, DEVELOPMENT_VERSION (%s, %s) in %s ci.yml" % (release_version, next_dev_version, project))
    text[rel_line] = rel_regex.sub("RELEASE_VERSION: %s" % release_version, text[rel_line])
    text[dev_line] = dev_regex.sub("DEVELOPMENT_VERSION: %s" % next_dev_version, text[dev_line])

    with open(filename, "w") as file:
        file.writelines(text)

    switch_dir("root")


def increment_schema(increment):
    project = "alfresco-community-repo"
    properties_path = os.path.join(project, "repository", "src", "main", "resources", "alfresco")
    filename = "repository.properties"
    switch_dir(properties_path)
    key = "version.schema="
    regex = re.compile(key + ".*", re.IGNORECASE)
    with open(filename, "r") as file:
        text = file.readlines()

    for i in range(len(text)):
        if key in text[i]:
            line_no = i

    schema = text[line_no].split("=")[1]
    new_schema = int(schema) + increment

    logger.debug("Updating property version.schema from %s to %s in %s" % (schema, new_schema, project))
    text[line_no] = regex.sub(key + str(new_schema), text[line_no])

    with open(filename, "w") as file:
        file.writelines(text)

    switch_dir("root")


def exec_cmd(cmd_args):
    logger.debug("Executing command line of %s" % " ".join(cmd_args))
    subprocess.run(cmd_args, shell=True) if logging.DEBUG == logger.level else subprocess.run(cmd_args, shell=True,
                                                                                              stdout=subprocess.DEVNULL)


def set_versions(project, version, profiles: list[str]):
    switch_dir(project)
    arguments = ["mvn", "versions:set", "-DgenerateBackupPoms=false", "-DnewVersion=%s" % version, "-P%s" % ",".join(profiles)]
    logger.debug("Updating versions to %s in pom of %s" % (version, project))

    exec_cmd(arguments)
    switch_dir("root")


def checkout_branch(project, branch):
    switch_dir(project)
    logger.debug("Checking out %s branch in %s" % (branch, project))
    exec_cmd(["git", "fetch"])
    exec_cmd(["git", "checkout", branch])
    switch_dir("root")


def create_branch(project, branch, tag):
    switch_dir(project)
    logger.debug("Creating %s branch in %s from %s tag" % (branch, project, tag))
    checkout_branch(project, tag)
    exec_cmd(["git", "switch", "-c", branch])
    switch_dir("root")


def commit_and_push(project, message):
    logger.debug("Committing changes in %s. Commit message: %s" % (project, message))
    switch_dir(project)
    exec_cmd(["git", "commit", "--all", "-m", message])
    if args.skip_push:
        logger.debug("Pushing changes in %s to remote." % project)
        exec_cmd(["git", "push"])
    switch_dir("root")

def calculate_hotfix_branch():
    rel_ver = release_version.split(".")
    rel_ver[2] = "N"
    prefix = "test/release/" if args.test_branches else "release/"
    return prefix + ".".join(rel_ver)

def calculate_hotfix_version(project):
    match project:
        case "acs-packaging" | "acs-community-packaging":
            return release_version
        case "alfresco-enterprise-repo":
            switch_dir("acs-packaging")
            ent_repo_ver = get_xml_tag_value("pom.xml", "{%s}properties/{%s}dependency.alfresco-enterprise-repo.version" % (POM_NS, POM_NS))
            switch_dir("root")
            return ent_repo_ver
        case "alfresco-enterprise-share":
            switch_dir("acs-packaging")
            ent_share_ver = get_xml_tag_value("pom.xml", "{%s}properties/{%s}dependency.alfresco-enterprise-share.version" % (POM_NS, POM_NS))
            switch_dir("root")
            return ent_share_ver
        case "alfresco-community-repo":
            switch_dir("alfresco-enterprise-repo")
            comm_repo_ver = get_xml_tag_value("pom.xml", "{%s}properties/{%s}dependency.alfresco-community-repo.version" % (POM_NS, POM_NS))
            switch_dir("root")
            return comm_repo_ver


def update_project(project, version, branch_type):
    set_versions(project, version, ["dev"])
    update_scm_tag("HEAD", project)
    if project.endswith("packaging"):
        update_ci_yaml(YAML_DICT.get(project), project, version, get_next_dev_version(branch_type))
    if project == "alfresco-community-repo":
        update_acs_ver_pom_properties(version, project)
        increment_schema(1)
    if project == "alfresco-enterprise-share":
        update_acs_ver_pom_properties(version, project)
    if project == "alfresco-enterprise-repo":
        switch_dir(project)
        xml_tree = load_xml("pom.xml")
        update_xml_tag(xml_tree, "{%s}properties/{%s}acs.version.label" % (POM_NS, POM_NS), ".1")

def create_hotfix_branches():
    hotfix_branch = calculate_hotfix_branch()
    branch_type = "hotfix"
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        tag_version = calculate_hotfix_version(project)
        create_branch(project, hotfix_branch, tag_version)
        version = increment_version(tag_version, branch_type) + "-SNAPSHOT"
        update_project(project, version, branch_type)


create_hotfix_branches()
