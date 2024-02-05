#######################################
# This script creates HotFix branches for repo, share and packaging projects, and updates the master branches ready for the next SP/major release.
# Run the script without passing any arguments or with -h/--help argument to get usage information.
# When the script is run with indicating that next version is a major bump (-r/--release version major is lowera than -n/--next_dev version major)
# then HF and SP branches get created. Otherwise (no major version bump), only HF branches are created.
# See below script behaviour explained.
#######################################
# Create a HotFix branches for the released version (for X.Y.Z release it will be release/X.Y.N eg., create release/23.2.N for 23.2.0 release)
# 1. acs-packaging:
# - set RELEASE_VERSION to X.Y.1, DEVELOPMENT_VERSION to X.Y.2-SNAPSHOT in master_release.yml
# - set POM versions to X.Y.1-SNAPSHOT
# - set scm-tag in main POM to HEAD
# 2. enterprise-share
# - set scm-tag in main POM to HEAD
# - set ACS version properties in main POM to X.Y.1
# - set POM versions to  X.Y.1.1-SNAPSHOT
# 3. enterprise-repo:
# - set POM versions to  X.Y.1.1-SNAPSHOT
# - set scm-tag in main POM to HEAD
# - set acs.version.label to .1
# 4. community-share
# - set scm-tag in main POM to HEAD
# - set ACS version properties in main POM to X.Y.1
# - set main POM versions to  X.Y.1.1-SNAPSHOT
# 5. community-repo:
# - set ACS version properties in main POM to X.Y.1
# - set POM versions to  X.Y.1.1-SNAPSHOT
# - set scm-tag in main POM to HEAD
# - increment schema by 1 in repository.properties
# - set version.revision to 1 in version.properties (test resources)
# 6. acs-community-packaging
# - not created as we do not release hot fixes for community version
#######################################
# Create a HotFix branches for the released version (for X.Y.Z release it will be release/X.Y+1.0 eg., create release/23.3.0 for 23.2.0 release)
# 1. acs-packaging:
# - set RELEASE_VERSION to X.Y+1.0-A1, DEVELOPMENT_VERSION to X.Y+1.0-A2-SNAPSHOT in master_release.yml
# - set POM versions to X.Y+1.0-SNAPSHOT
# - set scm-tag in main POM to HEAD
# 2. enterprise-share
# - set scm-tag in main POM to HEAD
# - set ACS version properties in main POM to X.Y+1.0
# - set POM versions to  X.Y+1.0.1-SNAPSHOT
# 3. enterprise-repo:
# - set POM versions to  X.Y+1.0.1-SNAPSHOT
# - set scm-tag in main POM to HEAD
# - set acs.version.label comment to <!-- X.Y+1.0.<acs.version.label> -->
# 4. community-share
# - set scm-tag in main POM to HEAD
# - set ACS version properties in main POM to X.Y+1.0
# - set main POM versions to  X.Y+1.0.1-SNAPSHOT
# 5. community-repo:
# - set ACS version properties in main POM to X.Y+1.0
# - set POM versions to  X.Y+1.0.1-SNAPSHOT
# - set scm-tag in main POM to HEAD
# - increment schema by 100 in repository.properties
# - set version.revision to Z+1 in version.properties (test resources)
# 6. acs-community-packaging
# - set RELEASE_VERSION to X.Y+1.0-A1, DEVELOPMENT_VERSION to X.Y+1.0-A2-SNAPSHOT in ci.yml
# - set POM versions to X.Y+1.0-SNAPSHOT
# - set scm-tag in main POM to HEAD
# - set comm-repo dependency in main POM to X.Y+1.0.1
# - set comm-share dependency in main POM to X.Y+1.0.1
#######################################
# Update master branch for the next SP/major release
# 1. acs-packaging:
# - set RELEASE_VERSION to <next_development_version>-A1 passed as script argument or X.Y+1.0-A1 (if <next_development_version> not passed),
#   DEVELOPMENT_VERSION to <next_development_version>-A2-SNAPSHOT or X.Y+1.0-A2-SNAPSHOT (if <next_development_version> not passed) in master_release.yml
# - set POM versions to <next_development_version>-SNAPSHOT or X.Y+1.0-SNAPSHOT (if <next_development_version> not passed)
# - set scm-tag in main POM to HEAD
# 2. enterprise-share
# - set scm-tag in main POM to HEAD
# - set ACS version properties in main POM to <next_development_version> or X.Y+1.0 (if <next_development_version> not passed)
# - set POM versions to <next_development_version>.1-SNAPSHOT or X.Y+1.0.1-SNAPSHOT (if <next_development_version> not passed)
# 3. enterprise-repo:
# - set POM versions to <next_development_version>.1-SNAPSHOT or X.Y+1.0.1-SNAPSHOT (if <next_development_version> not passed)
# - set scm-tag in main POM to HEAD
# - set acs.version.label comment to <!-- X.Y+1.0.<acs.version.label> -->
# 4. community-share
# - set scm-tag in main POM to HEAD
# - set ACS version properties in main POM to <next_development_version> or X.Y+1.0 (if <next_development_version> not passed)
# - set POM versions to <next_development_version>.1-SNAPSHOT or X.Y+1.0.1-SNAPSHOT (if <next_development_version> not passed)
# 5. community-repo:
# - set ACS version properties in main POM to <next_development_version> or X.Y+1.0 (if <next_development_version> not passed)
# - set POM versions to <next_development_version>.1-SNAPSHOT or X.Y+1.0.1-SNAPSHOT (if <next_development_version> not passed)
# - set scm-tag in main POM to HEAD
# - increment schema by 100 (when next development minor version bumped) or 1000 (when next development version major bumped) in repository.properties
# - set version.major/version.minor/version.revision to <next_development_version> or X.Y+1.0 (if <next_development_version> not passed) in version.properties (test resources)
# 6. acs-community-packaging
# - set RELEASE_VERSION to <next_development_version>-A1 passed as script argument or X.Y+1.0-A1 (if <next_development_version> not passed),
#   DEVELOPMENT_VERSION to <next_development_version>-A2-SNAPSHOT or X.Y+1.0-A2-SNAPSHOT (if <next_development_version> not passed) in master_release.yml
# - set scm-tag in main POM to HEAD
# - set comm-repo dependency in main POM to <next_development_version>.1 or X.Y+1.0.1 (if <next_development_version> not passed)
# - set comm-share dependency in main POM to <next_development_version>.1 or X.Y+1.0.1 (if <next_development_version> not passed)
#######################################

# !/usr/bin/env python

import argparse
import logging
import os
import re
import subprocess
import sys
from xml.etree import ElementTree as et

MASTER = 'master'
HOTFIX = 'hotfix'
SERVICE_PACK = 'service_pack'

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
parser.add_argument('-c', '--cleanup', action='store_true', help='cleanup local release branches (experimental)')
parser.add_argument('-a', '--ahead', action='store_true', help='create branches ahead of release (experimental)')
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
next_dev_version = args.next_dev if args.next_dev else input("Enter next development version (optional, use x.y or x.y.z format or leave empty): ")

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


def read_file(filename):
    with open(filename, 'r') as file:
        text = file.readlines()
    return text


def save_file(filename, text):
    with open(filename, 'w') as file:
        file.writelines(text)


def get_version_number(rel_ver, index):
    return int(rel_ver.split(".")[index])


def is_version_bumped(version, next_version, index):
    return get_version_number(version, index) < get_version_number(next_version, index) if next_version else False


def increment_version(rel_ver, branch_type):
    logger.debug("Incrementing %s version for %s branch" % (rel_ver, branch_type))
    versions = rel_ver.split(".")
    if branch_type == HOTFIX:
        incremented = get_version_number(rel_ver, 2) + 1
        versions[2] = str(incremented)
    if branch_type == SERVICE_PACK:
        incremented = get_version_number(rel_ver, 1) + 1
        versions[1] = str(incremented)
        versions[2] = "0"
    if branch_type == MASTER:
        is_major_bumped = is_version_bumped(rel_ver, next_dev_version, 0)
        incremented = get_version_number(rel_ver, 1) + 1
        versions[1] = str(incremented) if not is_major_bumped else next_dev_version.split(".")[1]
        versions[2] = "0" if not is_major_bumped else next_dev_version.split(".")[2]
        if is_major_bumped:
            versions[0] = next_dev_version.split(".")[0]

    if len(versions) == 4:
        versions.pop()
        versions.append("1")
    incremented_ver = ".".join(versions)
    logger.debug("Incremented to %s" % incremented_ver)
    return incremented_ver


def get_next_dev_version(type):
    if next_dev_version and type == MASTER:
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


def update_xml_tag(text, tag, new_value):
    closing_tag = tag.replace("<", "</")
    update_line(text, tag, "%s%s" % (new_value, closing_tag))
    return text


def load_xml(xml_path):
    logger.debug("Loading %s XML file" % xml_path)
    et.register_namespace("", POM_NS)
    xml_tree = et.parse(xml_path)
    xml_tree.parse(xml_path)
    return xml_tree


def update_acs_ver_pom_properties(project, version):
    prefix = "acs." if project == COMMUNITY_REPO else ""
    switch_dir(project)
    pom_path = "pom.xml"
    split_version = version.split(".")
    logger.debug("Setting ACS versions (%s) in %s pom.xml" % (split_version, project))
    text = read_file(pom_path)
    update_xml_tag(text, "<%sversion.major>" % prefix, split_version[0])
    update_xml_tag(text, "<%sversion.minor>" % prefix, split_version[1])
    update_xml_tag(text, "<%sversion.revision>" % prefix, split_version[2])
    save_file(pom_path, text)
    switch_dir("root")


def update_scm_tag(tag, project):
    switch_dir(project)
    pom_path = "pom.xml"
    logger.debug("Setting scm tag to %s in %s pom.xml" % (tag, project))
    text = read_file(pom_path)
    update_xml_tag(text, "<tag>", tag)
    save_file(pom_path, text)
    switch_dir('root')


def update_line(text: list[str], text_to_match, replacement_value):
    regex = re.compile(text_to_match + ".*", re.IGNORECASE)
    line = None
    for i in range(len(text)):
        if text_to_match in text[i]:
            line = i
    if line:
        text[line] = regex.sub("%s%s" % (text_to_match, replacement_value), text[line])

    return text


def update_ci_yaml(filename, project, rel_version, dev_version):
    ci_yaml_path = os.path.join(project, ".github", "workflows")
    switch_dir(ci_yaml_path)
    text = read_file(filename)

    release_version_match = "RELEASE_VERSION:"
    development_version_match = "DEVELOPMENT_VERSION:"

    if text:
        logger.debug("Setting RELEASE_VERSION, DEVELOPMENT_VERSION (%s, %s) in %s ci.yml" % (rel_version, dev_version, project))
        update_line(text, release_version_match, rel_version)
        update_line(text, development_version_match, dev_version)
        save_file(filename, text)

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
    text = read_file(filename)

    key = "version.schema="
    schema = int(read_property(text, key))
    new_schema = int(schema / increment) * increment + increment if increment == 1000 else schema + increment

    logger.debug("Updating property version.schema from %s to %s in %s" % (schema, new_schema, project))
    update_line(text, key, str(new_schema))

    save_file(filename, text)

    switch_dir('root')


def update_acs_comm_pck_dependencies(branch_type, project):
    logger.debug("Updating comm-repo and comm-share dependencies in %s" % project)
    comm_repo_next_ver = increment_version(calculate_version(COMMUNITY_REPO), branch_type)
    logger.debug("comm-repo dependency: %s" % comm_repo_next_ver)
    switch_dir(project)
    pom_path = "pom.xml"
    text = read_file(pom_path)
    update_xml_tag(text, "<dependency.alfresco-community-repo.version>", comm_repo_next_ver)
    update_xml_tag(text, "<version>\d", comm_repo_next_ver)
    comm_share_next_ver = increment_version(calculate_version(ENTERPRISE_SHARE), branch_type)
    logger.debug("comm-share dependency: %s" % comm_share_next_ver)
    switch_dir(project)
    update_xml_tag(text, "<dependency.alfresco-community-share.version>", comm_share_next_ver)
    save_file(pom_path, text)
    switch_dir('root')


def set_ags_test_versions(project, version):
    properties_path = os.path.join(project, "amps", "ags", "rm-community", "rm-community-repo", "test", "resources", "alfresco")
    filename = "version.properties"
    switch_dir(properties_path)
    text = read_file(filename)

    logger.debug("Updating versions to %s in version.properties file in %s" % (version, project))
    major_key = "version.major="
    update_line(text, major_key, version.split(".")[0])
    minor_key = "version.minor="
    update_line(text, minor_key, version.split(".")[1])
    revision_key = "version.revision="
    update_line(text, revision_key, version.split(".")[2])

    save_file(filename, text)

    switch_dir('root')


def calculate_increment(version, next_dev_ver):
    logger.debug("Calculating increment for version %s with next version %s" % (version, next_dev_ver))
    if is_version_bumped(version, next_dev_ver, 0):
        logger.debug("Increment by 1000")
        return 1000
    if is_version_bumped(version, next_dev_ver, 1):
        logger.debug("Increment by 100")
        return 100
    logger.debug("Increment by 1")
    return 1


def update_ent_repo_acs_label(project, version, branch_type):
    switch_dir(project)
    filename = "pom.xml"
    text = read_file(filename)
    versions = version.split(".")
    if len(versions) == 4:
        versions.pop()
    ver = ".".join(versions)

    if branch_type == HOTFIX:
        logger.debug("Setting acs.version.label to .1 in %s" % project)
        update_line(text, "<acs.version.label", ">.1</acs.version.label>")
    else:
        logger.debug("Setting acs.version.label comment to %s in %s" % (ver, project))
        update_line(text, "<acs.version.label", "/> <!-- %s.<acs.version.label> -->" % ver)

    save_file(filename, text)
    switch_dir('root')


def exec_cmd(cmd_args):
    logger.debug("Executing command line of %s" % " ".join(cmd_args))
    try:
        ret = subprocess.run(cmd_args, shell=True) if args.trace else subprocess.run(cmd_args, shell=True, stdout=subprocess.DEVNULL)
        ret.check_returncode()
    except subprocess.CalledProcessError as e:
        logger.error("Error:\nreturn code: %s\nOutput: %s" % (e.returncode, e.stderr.decode("utf-8")))
        raise


def get_cmd_exec_result(cmd_args):
    logger.debug("Getting results of command line execution of %s" % " ".join(cmd_args))
    try:
        return subprocess.check_output(cmd_args)
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


def commit_and_push(project, option, message):
    logger.debug("Committing changes in %s. Commit message: %s" % (project, message))
    switch_dir(project)
    exec_cmd(["git", "commit", option, "-m", message])
    if not args.skip_push:
        logger.debug("Pushing changes in %s to remote." % project)
        exec_cmd(["git", "push"])
    switch_dir('root')


def commit_all_and_push(project, message):
    commit_and_push(project, "--all", message)


def calculate_branch(type):
    rel_ver = release_version.split(".")
    if type == HOTFIX:
        rel_ver[2] = "N"
    elif type == SERVICE_PACK:
        rel_ver[1] = int(rel_ver[1]) + 1
        rel_ver.pop(2)
    else:
        rel_ver.pop(2)
    prefix = "test/release/" if args.test_branches else "release/"
    branch = prefix + ".".join(rel_ver)
    logger.debug("Calculated %s branch as %s " % (type, branch))
    return branch


def calculate_version(project):
    logger.debug("Calculating tag version for %s " % project)
    checkout_branch(ACS_PACKAGING, MASTER if args.ahead else release_version)
    switch_dir(ACS_PACKAGING)
    ent_repo_ver = get_xml_tag_value("pom.xml",
                                     "{%s}properties/{%s}dependency.alfresco-enterprise-repo.version" % (POM_NS, POM_NS))
    ent_share_ver = get_xml_tag_value("pom.xml",
                                      "{%s}properties/{%s}dependency.alfresco-enterprise-share.version" % (POM_NS, POM_NS))
    switch_dir('root')
    if project == ACS_PACKAGING or project == COMMUNITY_PACKAGING:
        logger.debug("Tag version for %s is %s" % (project, release_version))
        return release_version
    elif project == ENTERPRISE_REPO:
        logger.debug("Tag version for %s is %s" % (project, ent_repo_ver))
        return ent_repo_ver
    elif project == ENTERPRISE_SHARE:
        logger.debug("Tag version for %s is %s" % (project, ent_share_ver))
        return ent_share_ver
    elif project == COMMUNITY_REPO:
        checkout_branch(ENTERPRISE_REPO, ent_repo_ver)
        switch_dir(ENTERPRISE_REPO)
        comm_repo_ver = get_xml_tag_value("pom.xml",
                                          "{%s}properties/{%s}dependency.alfresco-community-repo.version" % (POM_NS, POM_NS))
        switch_dir('root')
        logger.debug("Tag version for %s is %s" % (project, comm_repo_ver))
        return comm_repo_ver


def update_project(project, version, branch_type):
    profiles = ["dev"] if "packaging" in project else ["ags"]
    set_versions(project, version, profiles)
    update_scm_tag('HEAD', project)
    next_dev_ver = get_next_dev_version(branch_type)
    if project == ACS_PACKAGING:
        update_ci_yaml(YAML_DICT.get(project), project, version + "-A1", next_dev_ver + "-A2-SNAPSHOT") if branch_type is not HOTFIX else (
            update_ci_yaml(YAML_DICT.get(project), project, version, increment_version(next_dev_ver, HOTFIX) + "-SNAPSHOT"))
    elif project == ENTERPRISE_SHARE:
        update_acs_ver_pom_properties(project, version)
    elif project == ENTERPRISE_REPO:
        update_ent_repo_acs_label(project, version, branch_type)
    elif project == COMMUNITY_REPO:
        update_acs_ver_pom_properties(project, version)
        increment_schema(project, calculate_increment(release_version, next_dev_ver))
        set_ags_test_versions(project, version)
    elif project == COMMUNITY_PACKAGING:
        update_ci_yaml(YAML_DICT.get(project), project, version + "-A1", next_dev_ver + "-A2-SNAPSHOT") if branch_type is not HOTFIX else (
            update_ci_yaml(YAML_DICT.get(project), project, version, increment_version(next_dev_ver, HOTFIX) + "-SNAPSHOT"))
        update_acs_comm_pck_dependencies(branch_type, project)


def log_progress(project, message):
    logger.info("---------------------------------------------")
    logger.info("Processing:")
    logger.info(project)
    logger.info("Message:")
    logger.info(message)
    logger.info("---------------------------------------------")


def create_hotfix_branches():
    hotfix_branch = calculate_branch(HOTFIX)
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        if project is not COMMUNITY_PACKAGING:
            log_progress(project, "Creating hotfix branches")
            rel_tag_version = calculate_version(project)
            create_branch(project, hotfix_branch, rel_tag_version)
            version = increment_version(rel_tag_version, HOTFIX)
            update_project(project, version, HOTFIX)
            commit_all_and_push(project, "Creating hotfix branch %s for %s ACS release [skip ci]" % (hotfix_branch, release_version))


def create_service_pack_branches():
    sp_branch = calculate_branch(SERVICE_PACK)
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        log_progress(project, "Creating service pack branches")
        rel_tag_version = calculate_version(project)
        create_branch(project, sp_branch, rel_tag_version)
        version = increment_version(rel_tag_version, SERVICE_PACK)
        update_project(project, version, SERVICE_PACK)
        commit_all_and_push(project, "Creating service pack branch %s after %s ACS release [skip ci]" % (sp_branch, release_version))


def modify_master_branches():
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        log_progress(project, "Updating master for next release")
        next_dev_ver = get_next_dev_version(MASTER)
        checkout_branch(project, MASTER)
        update_project(project, next_dev_ver, MASTER)
        commit_all_and_push(project, "Updating master branch to %s after %s ACS release [skip ci]" % (next_dev_ver, release_version))
        checkout_branch(project, MASTER)


def create_release_branches():
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        log_progress(project, "Creating release branches")
        rel_branch = calculate_branch('release')
        create_branch(project, rel_branch, MASTER)
        commit_and_push(project, "--allow-empty", "Creating release branch %s for %s ACS release [skip ci]" % (rel_branch, release_version))


def cleanup_branches():
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        log_progress(project, "Deleting test/release branches and resetting master to origin")
        checkout_branch(project, MASTER)
        switch_dir(project)
        exec_cmd(["git", "reset", "--hard", "origin/master"])
        stdout = get_cmd_exec_result(["git", "branch", "--list"])
        out = stdout.decode()
        branches = [b.strip('* ') for b in out.splitlines()]
        for b in branches:
            branch = str(b)
            if "test/release/" in branch:
                logger.debug("Deleting  %s branch" % branch)
                exec_cmd(["git", "branch", "-D", branch])


if args.cleanup:
    cleanup_branches()
    logger.info("Cleaned up test/release branches. Exiting.")
    sys.exit(0)


if release_version:
    if args.ahead:
        create_release_branches()
    else:
        create_hotfix_branches()
        if is_version_bumped(release_version, next_dev_version, 0):
            create_service_pack_branches()
    # need fix as below method will cause issues when this script is run with args.ahead and subsequently without it
    modify_master_branches()
    log_progress("All projects", "Finished creating branches. Exiting.")
    sys.exit(0)
