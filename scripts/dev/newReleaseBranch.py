#######################################
# This script creates HotFix branches for repo, share and packaging projects, and updates the master branches ready for the next SP/major release.
# Run the script without passing any arguments or with -h/--help argument to get usage information.
# When the script is run with indicating that next version is a major bump (-r/--release version major is lower than -n/--next_dev version major)
# then HF and SP branches get created. Otherwise (no major version bump), only HF branches are created.
# Script can also create release branches ahead of release so that master/main branches do not need to get the code frozem (-a/--ahead argument is passed)
# If -u argument provided then unit tests are run.
# If -m argument provided then master branch preparation is skipped (most likely to be used when preparing release branches ahead of release).
# See below script behaviour explained.
#######################################
# Create HotFix branches for the released version (for X.Y.Z release it will be release/X.Y eg., create release/25.2 for 25.2.0 release)
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
# 4. community-repo:
# - set ACS version properties in main POM to X.Y.1
# - set POM versions to  X.Y.1.1-SNAPSHOT
# - set scm-tag in main POM to HEAD
# - set version.revision to 1 in version.properties (test resources)
# 5. acs-community-packaging
# - not created as we do not release hot fixes for community version
#######################################
# Create ServicePack branches for the released version (for X.Y.Z release it will be release/X.N eg., create release/25.N for 25.3.0 release)
# 1. acs-packaging:
# - set RELEASE_VERSION to X.Y+1.0-A.1, DEVELOPMENT_VERSION to X.Y+1.0-A.2-SNAPSHOT in master_release.yml
# - set POM versions to X.Y+1.0-A.1-SNAPSHOT
# - set scm-tag in main POM to HEAD
# 2. enterprise-share
# - set scm-tag in main POM to HEAD
# - set ACS version properties in main POM to X.Y+1.0
# - set POM versions to  X.Y+1.0.1-SNAPSHOT
# 3. enterprise-repo:
# - set POM versions to  X.Y+1.0.1-SNAPSHOT
# - set scm-tag in main POM to HEAD
# - set acs.version.label comment to <!-- X.Y+1.0.<acs.version.label> -->
# 4. community-repo:
# - set ACS version properties in main POM to X.Y+1.0
# - set POM versions to  X.Y+1.0.1-SNAPSHOT
# - set scm-tag in main POM to HEAD
# - increment schema by 100 in repository.properties
# - set version.revision to Z+1 in version.properties (test resources)
# 5. acs-community-packaging
# - set RELEASE_VERSION to X.Y+1.0-A.1, DEVELOPMENT_VERSION to X.Y+1.0-A.2-SNAPSHOT in ci.yml
# - set POM versions to X.Y+1.0-A.1-SNAPSHOT
# - set scm-tag in main POM to HEAD
# - set comm-repo dependency in main POM to X.Y+1.0.1
# - set comm-share dependency in main POM to X.Y+1.0.1
#######################################
# Update master branch for the next SP/major release
# 1. acs-packaging:
# - set RELEASE_VERSION to <next_development_version>-A.1 passed as script argument or X.Y+1.0-A.1 (if <next_development_version> not passed),
#   DEVELOPMENT_VERSION to <next_development_version>-A.2-SNAPSHOT or X.Y+1.0-A.2-SNAPSHOT (if <next_development_version> not passed) in master_release.yml
# - set POM versions to <next_development_version>-A.1-SNAPSHOT or X.Y+1.0-A.1-SNAPSHOT (if <next_development_version> not passed)
# - set scm-tag in main POM to HEAD
# 2. enterprise-share
# - set scm-tag in main POM to HEAD
# - set ACS version properties in main POM to <next_development_version> or X.Y+1.0 (if <next_development_version> not passed)
# - set POM versions to <next_development_version>.1-SNAPSHOT or X.Y+1.0.1-SNAPSHOT (if <next_development_version> not passed)
# 3. enterprise-repo:
# - set POM versions to <next_development_version>.1-SNAPSHOT or X.Y+1.0.1-SNAPSHOT (if <next_development_version> not passed)
# - set scm-tag in main POM to HEAD
# - set acs.version.label comment to <!-- X.Y+1.0.<acs.version.label> -->
# 4. community-repo:
# - set ACS version properties in main POM to <next_development_version> or X.Y+1.0 (if <next_development_version> not passed)
# - set POM versions to <next_development_version>.1-SNAPSHOT or X.Y+1.0.1-SNAPSHOT (if <next_development_version> not passed)
# - set scm-tag in main POM to HEAD
# - increment schema by 100 (when next development minor version bumped) or 1000 (when next development version major bumped) in repository.properties
# - set version.major/version.minor/version.revision to <next_development_version> or X.Y+1.0 (if <next_development_version> not passed) in version.properties (test resources)
# 5. acs-community-packaging
# - set RELEASE_VERSION to <next_development_version>-A.1 passed as script argument or X.Y+1.0-A.1 (if <next_development_version> not passed),
#   DEVELOPMENT_VERSION to <next_development_version>-A.2-SNAPSHOT or X.Y+1.0-A.2-SNAPSHOT (if <next_development_version> not passed) in master_release.yml
# - set POM versions to <next_development_version>-A.1-SNAPSHOT or X.Y+1.0-A.1-SNAPSHOT (if <next_development_version> not passed)
# - set scm-tag in main POM to HEAD
# - set comm-repo dependency in main POM to <next_development_version>.1 or X.Y+1.0.1 (if <next_development_version> not passed)
# - set comm-share dependency in main POM to <next_development_version>.1 or X.Y+1.0.1 (if <next_development_version> not passed)
#######################################
# In case when release branches are to be created ahead of release (for X.Y.Z release it will be release/stabilization/X.Y eg., create release/stabilization/25.2 for 25.2.0 release)
# In all 5 projects, branches are created from master branch without any additional changes.
# Master branches are updated for the next SP/major release in a same way as in case of post release script execution (see above).
#######################################

# !/usr/bin/env python

import argparse
import logging
import os
import pathlib
import re
import subprocess
import sys
from xml.etree import ElementTree as et

MASTER = 'master'
HOTFIX = 'hotfix'
SERVICE_PACK = 'service_pack'
RELEASE = 'release'

POM_NS = 'http://maven.apache.org/POM/4.0.0'
COMMUNITY_PACKAGING = 'acs-community-packaging'
COMMUNITY_REPO = 'alfresco-community-repo'
ENTERPRISE_REPO = 'alfresco-enterprise-repo'
ENTERPRISE_SHARE = 'alfresco-enterprise-share'
ACS_PACKAGING = 'acs-packaging'
PROJECTS = [ACS_PACKAGING, ENTERPRISE_SHARE, ENTERPRISE_REPO, COMMUNITY_REPO, COMMUNITY_PACKAGING]

# read command line arguments
parser = argparse.ArgumentParser(description="Create git branches after ACS release.")
parser.add_argument('-r', '--release', metavar='x.y.z', help='release version (x.y.z format)')
parser.add_argument('-m', '--master_skip', action='store_true', help='skip prepare master branch')
parser.add_argument('-n', '--next_dev', metavar='x.y.z', help='next development version (x.y.z format)')
parser.add_argument('-v', '--verbose', action='store_true', help='print out verbose processing information')
parser.add_argument('-s', '--skip_push', action='store_true', help='skip git push')
parser.add_argument('-t', '--test_branches', action='store_true', help='use test branches')
parser.add_argument('-c', '--cleanup', action='store_true', help='cleanup local test release branches (experimental)')
parser.add_argument('-a', '--ahead', action='store_true', help='create branches ahead of release (experimental)')
parser.add_argument('-z', '--trace', action='store_true', help='trace processing information')
parser.add_argument('-u', '--unit_test', action='store_true', help='run unit tests')

if len(sys.argv) == 1:
    parser.print_help()
    exit(0)

# set global variables
script_directory = str(pathlib.Path(__file__).parent.resolve())
root_abspath = script_directory.split(ACS_PACKAGING)[0]

args = parser.parse_args()

release_version = ""
next_dev_version = ""
if not args.unit_test:
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

logger.debug(f"Released version: {release_version}")
logger.debug(f"Next development version: {next_dev_version}")
logger.debug(f"Skip push: {args.skip_push}")
logger.debug(f"Use test branches: {args.test_branches}")
logger.debug(f"Cleanup test branches: {args.cleanup}")


def read_file(filename):
    with open(filename, 'r') as file:
        text = file.readlines()
    return text


def save_file(filename, text):
    with open(filename, 'w') as file:
        file.writelines(text)


def get_version_number(rel_ver, index):
    """Get version form at index
    >>> get_version_number("23.1.0", 0)
    23
    >>> get_version_number("23.2.0", 1)
    2
    >>> get_version_number("23.1.4", 2)
    4
    """
    return int(rel_ver.split(".")[index])


def is_version_bumped(version, next_version, index):
    """Check if version at index has increased
    >>> is_version_bumped("23.3.0", "24.1.0", 0)
    True
    >>> is_version_bumped("23.2.0", "23.3.0", 1)
    True
    >>> is_version_bumped("23.1.0", "23.1.1", 2)
    True
    >>> is_version_bumped("23.3.0", "23.4.0", 0)
    False
    >>> is_version_bumped("23.2.0", "24.2.0", 1)
    False
    >>> is_version_bumped("23.1.0", "23.2.0", 2)
    False
    """
    return get_version_number(version, index) < get_version_number(next_version, index) if next_version else False


def increment_version(rel_ver, branch_type):
    """Increment version for branch type
    >>> increment_version("23.1.0", "hotfix")
    '23.1.1'
    >>> increment_version("23.1.0", "service_pack")
    '23.2.0'
    >>> increment_version("23.1.1", "service_pack")
    '23.2.0'
    >>> increment_version("23.1.0.85", "hotfix")
    '23.1.1.1'
    >>> increment_version("23.1.0.61", "service_pack")
    '23.2.0.1'
    >>> increment_version("23.1.1", "master")
    '23.2.0'
    """
    logger.debug(f"Incrementing {rel_ver} version for {branch_type} branch")
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
    logger.debug(f"Incremented to {incremented_ver}")
    return incremented_ver


def get_next_dev_version(branch_type, release_version=release_version, next_dev_version=next_dev_version):
    """Get the next development version for the given branch type
    >>> get_next_dev_version("master", release_version="23.1.1", next_dev_version="")
    '23.2.0'
    >>> get_next_dev_version("hotfix", release_version="23.1.1", next_dev_version="")
    '23.1.2'
    >>> get_next_dev_version("master", release_version="23.1.1", next_dev_version="24.1.0")
    '24.1.0'
    """
    if next_dev_version and branch_type == MASTER:
        logger.debug(f"Getting next dev version from input parameter ({next_dev_version})")
        return next_dev_version
    else:
        logger.debug(f"Getting next dev version. Incrementing released {release_version} for {branch_type}")
        return increment_version(release_version, branch_type)


def switch_dir(project):
    logger.debug(f"Current dir: {os.getcwd()}")
    os.chdir(os.path.dirname(root_abspath))
    if project != 'root':
        os.chdir(project)
    logger.debug(f"Switched dir to: {os.getcwd()}")


def get_xml_tag_value(xml_path, tag_path):
    xml_tree = load_xml(xml_path)
    return xml_tree.getroot().find(tag_path).text


def update_xml_tag(text, tag, new_value, match_after_regex=None):
    """
    >>> update_xml_tag(['   <url>http://github.com/Alfresco</url>', '   <tag>HEAD</tag>', ' </scm>'], "<tag>", "23.2.1")
    ['   <url>http://github.com/Alfresco</url>', '   <tag>23.2.1</tag>', ' </scm>']
    >>> update_xml_tag([" <artifactId>acs-comm-packaging</artifactId>", "  <version>23.1.0</version>","  <artifactId>something</artifactId>","    <version>${dep.version}</version>"], "<version>", "23.2.0")
    [' <artifactId>acs-comm-packaging</artifactId>', '  <version>23.2.0</version>', '  <artifactId>something</artifactId>', '    <version>${dep.version}</version>']
    >>> update_xml_tag([" <artifactId>acs-comm-packaging</artifactId>", "  <version>23.1.0</version>","  <artifactId>something</artifactId>","    <version>${dep.version}</version>"], "<version>", "23.2.0", "something")
    [' <artifactId>acs-comm-packaging</artifactId>', '  <version>23.1.0</version>', '  <artifactId>something</artifactId>', '    <version>23.2.0</version>']
    """
    closing_tag = tag.replace("<", "</")
    update_line(text, tag, new_value + closing_tag, match_after_regex)
    return text


def load_xml(xml_path):
    logger.debug(f"Loading {xml_path} XML file")
    et.register_namespace("", POM_NS)
    xml_tree = et.parse(xml_path)
    xml_tree.parse(xml_path)
    return xml_tree


def update_acs_ver_pom_properties(project, version):
    prefix = "acs." if project == COMMUNITY_REPO else ""
    switch_dir(project)
    pom_path = "pom.xml"
    split_version = version.split(".")
    logger.debug(f"Setting ACS versions ({split_version}) in {project} pom.xml")
    text = read_file(pom_path)
    update_xml_tag(text, f"<{prefix}version.major>", split_version[0])
    update_xml_tag(text, f"<{prefix}version.minor>", split_version[1])
    update_xml_tag(text, f"<{prefix}version.revision>", split_version[2])
    save_file(pom_path, text)
    switch_dir("root")


def update_scm_tag(tag, project):
    switch_dir(project)
    pom_path = "pom.xml"
    logger.debug(f"Setting scm tag to {tag} in {project} pom.xml")
    text = read_file(pom_path)
    update_xml_tag(text, "<tag>", tag)
    save_file(pom_path, text)
    switch_dir('root')


def update_line(text: list[str], text_to_match, replacement_value, match_after_regex=None):
    """Update part of line after matching text with given value
    >>> update_line(["BASE_BUILD_NUMBER: 10000", "RELEASE_VERSION: 23.1.0", "DEVELOPMENT_VERSION: 23.2.0-A1-SNAPSHOT"], "RELEASE_VERSION: ", "23.2.0")
    ['BASE_BUILD_NUMBER: 10000', 'RELEASE_VERSION: 23.2.0', 'DEVELOPMENT_VERSION: 23.2.0-A1-SNAPSHOT']
    >>> update_line(["BASE_BUILD_NUMBER: 10000", "RELEASE_VERSION: 23.1.0", "DEVELOPMENT_VERSION: 23.1.0-SNAPSHOT"], "DEVELOPMENT_VERSION: ", "23.2.0-A1-SNAPSHOT")
    ['BASE_BUILD_NUMBER: 10000', 'RELEASE_VERSION: 23.1.0', 'DEVELOPMENT_VERSION: 23.2.0-A1-SNAPSHOT']
    >>> update_line(["repository.name=Main Repository", "version.schema=19000", "dir.root=./alf_data"], "version.schema=", "19100")
    ['repository.name=Main Repository', 'version.schema=19100', 'dir.root=./alf_data']
    >>> update_line(["<url>http://github.com/Alfresco</url>", "<tag>23.1.0</tag>", "</scm>"], "<tag>", "HEAD</tag>")
    ['<url>http://github.com/Alfresco</url>', '<tag>HEAD</tag>', '</scm>']
    >>> update_line(["version.label=", "version.major=23", "version.minor=2", "version.revision=0"], "version.major=", "24")
    ['version.label=', 'version.major=24', 'version.minor=2', 'version.revision=0']
    >>> update_line(["version.major=23", "version.minor=2", "version.revision=0"], "version.minor=", "3")
    ['version.major=23', 'version.minor=3', 'version.revision=0']
    >>> update_line(["version.major=23", "version.minor=2", "version.revision=0"], "version.revision=", "1")
    ['version.major=23', 'version.minor=2', 'version.revision=1']
    >>> update_line(["<dependency.alfresco-community-repo.version>23.2.0.1</dependency.alfresco-community-repo.version>", "<acs.version.label /> <!-- 23.1.0.<acs.version.label> -->", "<version.edition>Enterprise</version.edition>"], "<acs.version.label", ">.1</acs.version.label>")
    ['<dependency.alfresco-community-repo.version>23.2.0.1</dependency.alfresco-community-repo.version>', '<acs.version.label>.1</acs.version.label>', '<version.edition>Enterprise</version.edition>']
    >>> update_line(["<dependency.alfresco-community-repo.version>23.2.0.1</dependency.alfresco-community-repo.version>", "<acs.version.label>.1</acs.version.label>", "<version.edition>Enterprise</version.edition>"], "<acs.version.label", "/> <!-- 23.2.0.<acs.version.label> -->")
    ['<dependency.alfresco-community-repo.version>23.2.0.1</dependency.alfresco-community-repo.version>', '<acs.version.label/> <!-- 23.2.0.<acs.version.label> -->', '<version.edition>Enterprise</version.edition>']
    >>> update_line(['  <version>23.1.0</version>'], "<version>", "23.2.0</version>")
    ['  <version>23.2.0</version>']
    >>> update_line([' <artifactId>acs-packaging</artifactId>', '  <version>23.1.0</version>', '  <artifactId>something</artifactId>', '    <version>${dep.version}</version>'], "<version>", "23.2.0</version>")
    [' <artifactId>acs-packaging</artifactId>', '  <version>23.2.0</version>', '  <artifactId>something</artifactId>', '    <version>${dep.version}</version>']
    >>> update_line([' <artifactId>acs-packaging</artifactId>', '  <version>23.1.0</version>', '  <artifactId>something</artifactId>', '    <version>${dep.version}</version>'], "<version>", "23.2.0</version>", "<artifactId>something</artifactId>")
    [' <artifactId>acs-packaging</artifactId>', '  <version>23.1.0</version>', '  <artifactId>something</artifactId>', '    <version>23.2.0</version>']
    >>> update_line([' <artifactId>acs-packaging</artifactId>', '  <version>23.1.0</version>', '  <artifactId>something</artifactId>', '    <version>${dep.version}</version>'], "<version>", "23.2.0</version>", "String not in text")
    [' <artifactId>acs-packaging</artifactId>', '  <version>23.1.0</version>', '  <artifactId>something</artifactId>', '    <version>${dep.version}</version>']
    """
    start_search = True if match_after_regex == None else False
    regex = re.compile(text_to_match + ".*", re.IGNORECASE)
    match_index = None
    for i, line in enumerate(text):
        if not start_search:
            if re.findall(match_after_regex, line, re.IGNORECASE):
                start_search = True
        elif text_to_match in line:
            match_index = i
            break
    if match_index != None:
        text[match_index] = regex.sub(text_to_match + replacement_value, text[match_index])

    return text


def update_ci_yaml(filename, project, rel_version, dev_version):
    ci_yaml_path = os.path.join(project, ".github", "workflows")
    switch_dir(ci_yaml_path)
    text = read_file(filename)

    release_version_match = "RELEASE_VERSION: "
    development_version_match = "DEVELOPMENT_VERSION: "

    if text:
        logger.debug(f"Setting RELEASE_VERSION, DEVELOPMENT_VERSION ({rel_version}, {dev_version}) in {project} ci.yml")
        update_line(text, release_version_match, rel_version)
        update_line(text, development_version_match, dev_version)
        save_file(filename, text)

    switch_dir('root')


def read_property(text: list[str], key):
    """Read property value given its key
    >>> read_property(["key1=value1", "key2=value2", "key3=value3"], "key3")
    'value3'
    >>> read_property(["key1=value1", "key2=value2", "key3=value3"], "key2")
    'value2'
    """
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

    logger.debug(f"Updating property version.schema from {schema} to {new_schema} in {project}")
    update_line(text, key, str(new_schema))

    save_file(filename, text)

    switch_dir('root')


def update_acs_comm_pck_dependencies(branch_type, project):
    logger.debug(f"Updating comm-repo and comm-share dependencies in {project}")
    comm_repo_next_ver = increment_version(calculate_version(COMMUNITY_REPO), branch_type)
    logger.debug(f"comm-repo dependency: {comm_repo_next_ver}")
    switch_dir(project)
    pom_path = "pom.xml"
    text = read_file(pom_path)
    update_xml_tag(text, "<dependency.alfresco-community-repo.version>", comm_repo_next_ver)
    update_xml_tag(text, "<version>", comm_repo_next_ver, '<parent>')
    comm_share_next_ver = increment_version(calculate_version(ENTERPRISE_SHARE), branch_type)
    logger.debug(f"comm-share dependency: {comm_share_next_ver}")
    switch_dir(project)
    update_xml_tag(text, "<dependency.alfresco-community-share.version>", comm_share_next_ver)
    save_file(pom_path, text)
    switch_dir('root')


def set_ags_test_versions(project, version):
    properties_path = os.path.join(project, "amps", "ags", "rm-community", "rm-community-repo", "test", "resources", "alfresco")
    filename = "version.properties"
    switch_dir(properties_path)
    text = read_file(filename)

    logger.debug(f"Updating versions to {version} in version.properties file in {project}")
    major_key = "version.major="
    update_line(text, major_key, version.split(".")[0])
    minor_key = "version.minor="
    update_line(text, minor_key, version.split(".")[1])
    revision_key = "version.revision="
    update_line(text, revision_key, version.split(".")[2])

    save_file(filename, text)

    switch_dir('root')


def calculate_increment(version, next_dev_ver):
    """Calculate version schema increment
    >>> calculate_increment("23.1.0", "23.1.1")
    0
    >>> calculate_increment("23.1.0", "23.2.0")
    100
    >>> calculate_increment("23.1.0", "24.1.0")
    1000
    """
    logger.debug(f"Calculating increment for version {version} with next version {next_dev_ver}")
    if is_version_bumped(version, next_dev_ver, 0):
        logger.debug("Increment by 1000")
        return 1000
    if is_version_bumped(version, next_dev_ver, 1):
        logger.debug("Increment by 100")
        return 100
    logger.debug("Increment by 0")
    return 0


def update_ent_repo_acs_label(project, version, branch_type):
    switch_dir(project)
    filename = "pom.xml"
    text = read_file(filename)
    versions = version.split(".")
    if len(versions) == 4:
        versions.pop()
    ver = ".".join(versions)

    if branch_type == HOTFIX:
        logger.debug(f"Setting acs.version.label to .1 in {project}")
        update_line(text, "<acs.version.label", ">.1</acs.version.label>")
    else:
        logger.debug(f"Setting acs.version.label comment to {ver} in {project}")
        update_line(text, "<acs.version.label", f"/> <!-- {ver}.<acs.version.label> -->")

    save_file(filename, text)
    switch_dir('root')


def exec_cmd(cmd_args):
    logger.debug("Executing command line of " + " ".join(cmd_args))
    try:
        ret = subprocess.run(cmd_args, shell=True) if args.trace else subprocess.run(cmd_args, shell=True, stdout=subprocess.DEVNULL)
        ret.check_returncode()
    except subprocess.CalledProcessError as e:
        logger.error(f"Error:\nreturn code: {e.returncode}\nOutput: " + e.stderr.decode("utf-8"))
        raise


def get_cmd_exec_result(cmd_args):
    logger.debug("Getting results of command line execution of " + " ".join(cmd_args))
    try:
        return subprocess.check_output(cmd_args)
    except subprocess.CalledProcessError as e:
        logger.error(f"Error:\nreturn code: {e.returncode}\nOutput: " + e.stderr.decode("utf-8"))
        raise


def set_versions(project, version, branch_type):
    profiles = ["dev"] if "packaging" in project else ["ags"]
    switch_dir(project)
    if "packaging" in project:
        snapshot_ver = version + "-SNAPSHOT" if branch_type == HOTFIX else version + "-A.1-SNAPSHOT"
    else:
        ver = version.split(".")
        if len(ver) == 4:
            ver.pop()
        snapshot_ver = ".".join(ver) + ".1-SNAPSHOT"

    arguments = ["mvn versions:set -DgenerateBackupPoms=false -DnewVersion=" + snapshot_ver + " -P" + ",".join(profiles)]
    logger.debug(f"Updating versions to {snapshot_ver} in pom of {project}")
    exec_cmd(arguments)
    switch_dir('root')


def checkout_branch(project, branch):
    switch_dir(project)
    logger.debug(f"Checking out {branch} branch in {project}")
    exec_cmd(["git fetch"])
    exec_cmd(["git checkout " + branch])
    switch_dir('root')


def create_branch(project, branch, tag):
    switch_dir(project)
    checkout_branch(project, tag)
    switch_dir(project)
    logger.debug(f"Creating {branch} branch in {project} from {tag} tag")
    exec_cmd(["git switch -c " + branch])
    switch_dir('root')


def commit_and_push(project, option, message):
    logger.debug(f"Committing changes in {project}. Commit message: {message}")
    switch_dir(project)
    exec_cmd(["git commit " + option + " -m \"" + message + "\""])
    if not args.skip_push:
        logger.debug(f"Pushing changes in {project} to remote.")
        exec_cmd(["git push"])
    switch_dir('root')


def commit_all_and_push(project, message):
    commit_and_push(project, "--all", message)


def calculate_branch(branch_type, release_version=release_version, use_test_branches=args.test_branches):
    """Calculate the branch name
    >>> calculate_branch("master", release_version="23.2.0")
    'master'
    >>> calculate_branch("service_pack", release_version="23.3.0")
    'release/23.N'
    >>> calculate_branch("service_pack", release_version="24.1.0")
    'release/24.N'
    >>> calculate_branch("hotfix", release_version="24.1.0")
    'release/24.1'
    >>> calculate_branch("hotfix", release_version="24.1.0", use_test_branches=True)
    'test/release/24.1'
    >>> calculate_branch("release", release_version="23.2.0")
    'release/stabilization/23.2'
    >>> calculate_branch("release", release_version="23.2.0", use_test_branches=True)
    'test/release/stabilization/23.2'
    """
    if branch_type == MASTER:
        return MASTER
    rel_ver = release_version.split(".")
    if branch_type == SERVICE_PACK:
        rel_ver[1] = "N"
    rel_ver.pop(2)
    prefix = "test/release/" if use_test_branches else "release/"
    if branch_type == RELEASE:
        prefix += "stabilization/"
    branch = prefix + ".".join(rel_ver)
    logger.debug(f"Calculated {branch_type} branch as {branch}")
    return branch


def calculate_version(project):
    logger.debug(f"Calculating tag version for {project}")
    checkout_branch(ACS_PACKAGING, MASTER if args.ahead else release_version)
    switch_dir(ACS_PACKAGING)
    ent_repo_ver = get_xml_tag_value("pom.xml",
                                     "{%s}properties/{%s}dependency.alfresco-enterprise-repo.version" % (POM_NS, POM_NS))
    ent_share_ver = get_xml_tag_value("pom.xml",
                                      "{%s}properties/{%s}dependency.alfresco-enterprise-share.version" % (POM_NS, POM_NS))
    switch_dir('root')
    if project == ACS_PACKAGING or project == COMMUNITY_PACKAGING:
        logger.debug(f"Tag version for {project} is {release_version}")
        return release_version
    elif project == ENTERPRISE_REPO:
        logger.debug(f"Tag version for {project} is {ent_repo_ver}")
        return ent_repo_ver
    elif project == ENTERPRISE_SHARE:
        logger.debug(f"Tag version for {project} is {ent_share_ver}")
        return ent_share_ver
    elif project == COMMUNITY_REPO:
        checkout_branch(ENTERPRISE_REPO, ent_repo_ver)
        switch_dir(ENTERPRISE_REPO)
        comm_repo_ver = get_xml_tag_value("pom.xml",
                                          "{%s}properties/{%s}dependency.alfresco-community-repo.version" % (POM_NS, POM_NS))
        switch_dir('root')
        logger.debug(f"Tag version for {project} is {comm_repo_ver}")
        return comm_repo_ver


def update_project(project, version, branch_type):
    set_versions(project, version, branch_type)
    update_scm_tag('HEAD', project)
    next_dev_ver = get_next_dev_version(branch_type)
    if project == ACS_PACKAGING:
        if branch_type is not HOTFIX:
            update_ci_yaml('master_release.yml', project, version + "-A.1", next_dev_ver + "-A.2-SNAPSHOT")
        else:
            update_ci_yaml('master_release.yml', project, version, increment_version(next_dev_ver, HOTFIX) + "-SNAPSHOT")
    elif project == ENTERPRISE_SHARE:
        update_acs_ver_pom_properties(project, version)
    elif project == ENTERPRISE_REPO:
        update_ent_repo_acs_label(project, version, branch_type)
    elif project == COMMUNITY_REPO:
        update_acs_ver_pom_properties(project, version)
        increment_schema(project, calculate_increment(release_version, next_dev_ver))
        set_ags_test_versions(project, version)
    elif project == COMMUNITY_PACKAGING:
        if branch_type is not HOTFIX:
            update_ci_yaml('ci.yml', project, version + "-A.1", next_dev_ver + "-A.2-SNAPSHOT")
        else:
            update_ci_yaml('ci.yml', project, version, increment_version(next_dev_ver, HOTFIX) + "-SNAPSHOT")
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
            commit_all_and_push(project, f"Creating hotfix branch {hotfix_branch} for {release_version} ACS release [skip ci]")


def create_service_pack_branches():
    sp_branch = calculate_branch(SERVICE_PACK)
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        log_progress(project, "Creating service pack branches")
        rel_tag_version = calculate_version(project)
        create_branch(project, sp_branch, rel_tag_version)
        version = increment_version(rel_tag_version, SERVICE_PACK)
        update_project(project, version, SERVICE_PACK)
        commit_all_and_push(project, f"Creating service pack branch {sp_branch} after {release_version} ACS release [skip ci]")


def modify_master_branches():
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        log_progress(project, "Updating master for next release")
        next_dev_ver = get_next_dev_version(MASTER)
        checkout_branch(project, MASTER)
        update_project(project, next_dev_ver, MASTER)
        commit_all_and_push(project, f"Updating master branch to {next_dev_ver} after {release_version} ACS release [skip ci]")
        checkout_branch(project, MASTER)


def create_release_branches():
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        log_progress(project, "Creating release branches")
        rel_branch = calculate_branch(RELEASE)
        create_branch(project, rel_branch, MASTER)
        commit_and_push(project, "--allow-empty", f"Creating release branch {rel_branch} for {release_version} ACS release [skip ci]")


def cleanup_branches():
    for i in range(len(PROJECTS)):
        project = PROJECTS[i]
        log_progress(project, "Deleting test/release branches and resetting master to origin")
        checkout_branch(project, MASTER)
        switch_dir(project)
        exec_cmd(["git reset --hard origin/master"])
        stdout = get_cmd_exec_result(["git", "branch", "--list"])
        out = stdout.decode()
        branches = [b.strip('* ') for b in out.splitlines()]
        for b in branches:
            branch = str(b)
            if "test/release/" in branch:
                logger.debug(f"Deleting  {branch} branch")
                exec_cmd(["git branch -D " + branch])

if args.unit_test:
    import doctest
    doctest.testmod()
    exit(0)

if args.cleanup:
    cleanup_branches()
    logger.info("Cleaned up test/release branches. Exiting.")
    exit(0)


if release_version:
    if args.ahead:
        create_release_branches()
    else:
        create_hotfix_branches()
        if is_version_bumped(release_version, next_dev_version, 0):
            create_service_pack_branches()
    if not args.master_skip:
        modify_master_branches()
    log_progress("All projects", "Finished creating branches. Exiting.")
    exit(0)
