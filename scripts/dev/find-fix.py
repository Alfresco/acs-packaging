#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse, os, re, subprocess
from collections import defaultdict

script_path = os.path.dirname(os.path.realpath(__file__))
root_dir = os.path.dirname(os.path.dirname(os.path.dirname(script_path)))

projects = ['alfresco-community-repo', 'alfresco-enterprise-repo', 'alfresco-enterprise-share', 'acs-community-packaging', 'acs-packaging']
project_dependencies = {'alfresco-community-repo': ['alfresco-enterprise-repo', 'acs-community-packaging'], 'alfresco-enterprise-repo': ['acs-packaging'], 'alfresco-enterprise-share': ['acs-packaging']}
project_dir = {project: os.path.join(root_dir, project) for project in projects}
version_string = {project: '<dependency.{0}.version>{{}}</dependency.{0}.version>'.format(project) for project in projects}

parser = argparse.ArgumentParser(description='Find the tags that contain commits referencing a ticket.')
parser.add_argument('-j', '--jira', help='The ticket number to search for.', required=True)
parser.add_argument('-a', '--all', action='store_true', help='Display all releases containing fix.')
parser.add_argument('-r', '--release', action='store_true', help='Only consider full releases.')
parser.add_argument('-p', '--packaged', action='store_true', help='Include information about how the commit is packaged.')
parser.add_argument('-s', '--skipfetch', action='store_true', help='Skip the git fetch step - only include commits that are stored locally.')
args = parser.parse_args()

# The filter to use to avoid considering test tags.
version_filter = r'^[0-9]+(\.[0-9]+)*$' if args.release else r'^[0-9]+(\.[0-9]+)*(-(A|M|RC)[0-9]+)?$'

def run_command(command_parts, project):
    """Run the command and return the output string."""
    output = subprocess.run(command_parts, cwd=project_dir[project], capture_output=True)
    return output.stdout.decode('utf-8')

def run_list_command(command_parts, project):
    """Run the command and return the lines of the output as a list of strings."""
    output = run_command(command_parts, project).strip().split('\n')
    if '' in output:
        output.remove('')
    return output

def compare_version_part(a_part, b_part):
    """Compare two parts of a version number and return the difference (taking into account
    versions like 7.0.0-M1)."""
    try:
        a_bits = a_part.split('-')
        b_bits = b_part.split('-')
        version_difference = int(a_bits[0]) - int(b_bits[0])
        if version_difference != 0 or (len(a_bits) == 1 and len(b_bits) == 1):
            return version_difference
        if len(a_bits) != len(b_bits):
            # Fewer parts indicates a later version (e.g. '7.0.0' is later than '7.0.0-M1')
            return len(b_bits) - len(a_bits)
        # If letter doesn't match then we can't compare the versions.
        a_letter = a_bits[1][0]
        b_letter = b_bits[1][0]
        if a_letter != b_letter:
            return 0
        # Try to get number from after M, A or RC and compare this.
        a_number_start = [char.isdigit() for char in a_bits[1]].index(True)
        b_number_start = [char.isdigit() for char in b_bits[1]].index(True)
        return int(a_bits[1][a_number_start:]) - int(b_bits[1][b_number_start:])
    except ValueError:
        # If the strings aren't in the format we're expecting then we can't compare them.
        return 0

def tag_before(tag_a, tag_b):
    """Return True if the version number from tag_a is lower than tag_b."""
    a_parts = list(tag_a.split('.'))
    b_parts = list(tag_b.split('.'))
    for part_index, b_part in enumerate(b_parts):
        if len(a_parts) <= part_index:
            return True
        difference = compare_version_part(a_parts[part_index], b_part)
        if difference < 0:
            return True
        elif difference > 0:
            return False
    return len(a_parts) <= len(b_parts)

def reduce_tags(tags):
    """Filter a set of tags to return only those that aren't descendents from others in the list."""
    reduced_tags = []
    for tag_a in tags:
        include = True
        for tag_b in tags:
            if tag_a == tag_b:
                continue
            if not tag_before(tag_a, tag_b):
                include = False
                break
        if include:
            reduced_tags.append(tag_a)
    return reduced_tags

def find_tags_containing(project, commit):
    """Find all tags containing the given commit.  Returns the full list and a condensed list (excluding tags 'after' other tags in the list)."""
    tags = run_list_command(['git', 'tag', '--contains', commit], project)
    # The packaging projects had a different format for older tags.
    if project in ['acs-packaging', 'acs-community-packaging']:
        # Remove the prefix 'acs-packaging-' if it's present.
        tags = list(map(lambda tag: tag.replace('{}-'.format(project), ''), tags))
    # Exclude tags that aren't just chains of numbers with an optional suffix.
    tags = list(filter(lambda tag: re.match(version_filter, tag), tags))
    # Filter out tags that are before other tags.
    reduced_tags = reduce_tags(tags)
    return tags, reduced_tags

for project in projects:
    if not args.skipfetch:
        run_command(['git', 'fetch'], project)
    commits = run_list_command(['git', 'rev-list', '--all', '--grep', args.jira], project)
    for original_commit in commits:
        tags, reduced_tags = find_tags_containing(project, original_commit)
        tag_info = ', '.join(tags if args.all else reduced_tags)
        packaging_info = ''
        if args.packaged:
            pairs = [(project, tag) for tag in reduced_tags]
            packaged = defaultdict(set)
            while pairs:
                dependency, tag = pairs.pop()
                if dependency not in project_dependencies.keys():
                    packaged[dependency].add(tag)
                else:
                    # Try to find pairs from next project up.
                    for ancestor_project in project_dependencies[dependency]:
                        commits = run_list_command(['git', 'log', '--all', '--pretty=format:%h', '-S', version_string[dependency].format(tag), '--', 'pom.xml'], ancestor_project)
                        for commit in commits:
                            _, found_tags = find_tags_containing(ancestor_project, commit)
                            pairs += [(ancestor_project, found_tag) for found_tag in found_tags]
                            pairs = list(set(pairs))
            if packaged:
                packaged_set = set()
                for packaging_project, packaged_tags in packaged.items():
                    for packaged_tag in reduce_tags(packaged_tags):
                        packaged_set.add((packaging_project, packaged_tag))
                packaging_info = ' ({})'.format(', '.join('{}:{}'.format(*pair) for pair in packaged_set))
        print('{:.7s} is in {}: {}{}'.format(original_commit, project, tag_info, packaging_info))
