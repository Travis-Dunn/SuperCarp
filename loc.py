#!/usr/bin/env python3
"""
loc - Lines of Code Counter

Recursively scans directories for source code files and counts:
- Lines of code (excluding comments and whitespace)
- Comment lines
- Blank lines
- Total lines
"""

import os
import sys
import re
from pathlib import Path
from collections import defaultdict

# File extensions and their comment styles
LANGUAGES = {
    # C-style comments: // and /* */
    '.c': 'c', '.h': 'c', '.cpp': 'c', '.hpp': 'c', '.cc': 'c',
    '.java': 'c', '.js': 'c', '.ts': 'c', '.jsx': 'c', '.tsx': 'c',
    '.cs': 'c', '.go': 'c', '.swift': 'c', '.kt': 'c', '.scala': 'c',
    '.rs': 'c', '.m': 'c', '.mm': 'c',
    # Hash comments: #
    '.py': 'hash', '.rb': 'hash', '.pl': 'hash', '.sh': 'hash',
    '.bash': 'hash', '.zsh': 'hash', '.r': 'hash', '.R': 'hash',
    '.yaml': 'hash', '.yml': 'hash', '.toml': 'hash',
    # Other
    '.sql': 'sql',  # -- and /* */
    '.lua': 'lua',  # -- and --[[ ]]
    '.hs': 'haskell',  # -- and {- -}
    '.html': 'html', '.xml': 'html', '.svg': 'html',  # <!-- -->
    '.css': 'css', '.scss': 'css', '.less': 'css',  # /* */
}

# Directories to skip
SKIP_DIRS = {
    '.git', '.svn', '.hg', 'node_modules', '__pycache__', '.idea',
    'venv', '.venv', 'env', '.env', 'build', 'dist', 'target',
    '.gradle', '.mvn', 'bin', 'obj', '.vs', '.vscode',
}


def count_lines(filepath, comment_style):
    """Count code, comment, and blank lines in a file."""
    try:
        with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
            lines = f.readlines()
    except (IOError, OSError):
        return None

    total = len(lines)
    blank = 0
    comment = 0
    code = 0
    in_block_comment = False
    block_start = None
    block_end = None

    # Set up block comment delimiters
    if comment_style == 'c':
        block_start, block_end = '/*', '*/'
        line_comment = '//'
    elif comment_style == 'sql':
        block_start, block_end = '/*', '*/'
        line_comment = '--'
    elif comment_style == 'lua':
        block_start, block_end = '--[[', ']]'
        line_comment = '--'
    elif comment_style == 'haskell':
        block_start, block_end = '{-', '-}'
        line_comment = '--'
    elif comment_style == 'html':
        block_start, block_end = '<!--', '-->'
        line_comment = None
    elif comment_style == 'css':
        block_start, block_end = '/*', '*/'
        line_comment = None
    elif comment_style == 'hash':
        block_start, block_end = None, None
        line_comment = '#'
    else:
        block_start, block_end = None, None
        line_comment = None

    for line in lines:
        stripped = line.strip()

        # Blank line
        if not stripped:
            blank += 1
            continue

        # Handle block comments
        if in_block_comment:
            comment += 1
            if block_end and block_end in stripped:
                in_block_comment = False
            continue

        # Check for block comment start
        if block_start and block_start in stripped:
            # Check if it's a single-line block comment
            if block_end and block_end in stripped:
                # Could have code before/after the comment
                before = stripped.split(block_start)[0].strip()
                after_parts = stripped.split(block_end)
                after = after_parts[-1].strip() if len(after_parts) > 1 else ''
                if before or after:
                    code += 1
                else:
                    comment += 1
            else:
                in_block_comment = True
                # Check if there's code before the comment
                before = stripped.split(block_start)[0].strip()
                if before and (not line_comment or not before.startswith(line_comment)):
                    code += 1
                else:
                    comment += 1
            continue

        # Check for line comment
        if line_comment and stripped.startswith(line_comment):
            comment += 1
            continue

        # It's code (might have trailing comment, but line contains code)
        code += 1

    return {'total': total, 'code': code, 'comment': comment, 'blank': blank}


def find_source_files(root_path):
    """Find all source code files in directory tree."""
    files = []
    root = Path(root_path)

    for dirpath, dirnames, filenames in os.walk(root):
        # Remove directories we want to skip
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS and not d.startswith('.')]

        for filename in filenames:
            ext = Path(filename).suffix.lower()
            if ext in LANGUAGES:
                files.append((Path(dirpath) / filename, ext))

    return files


def format_number(n):
    """Format number with thousand separators."""
    return f"{n:,}"


def main():
    # Determine root directory
    if len(sys.argv) > 1:
        root = sys.argv[1]
    else:
        root = '.'

    if not os.path.isdir(root):
        print(f"Error: '{root}' is not a directory", file=sys.stderr)
        sys.exit(1)

    # Find all source files
    files = find_source_files(root)

    if not files:
        print("No source files found.")
        sys.exit(0)

    # Count lines in each file
    totals = {'total': 0, 'code': 0, 'comment': 0, 'blank': 0}
    by_ext = defaultdict(lambda: {'files': 0, 'total': 0, 'code': 0, 'comment': 0, 'blank': 0})
    file_count = 0
    skipped = 0

    for filepath, ext in files:
        comment_style = LANGUAGES[ext]
        result = count_lines(filepath, comment_style)

        if result is None:
            skipped += 1
            continue

        file_count += 1
        by_ext[ext]['files'] += 1

        for key in totals:
            totals[key] += result[key]
            by_ext[ext][key] += result[key]

    # Print results
    print()
    print("=" * 70)
    print(f"  Lines of Code Summary: {os.path.abspath(root)}")
    print("=" * 70)
    print()

    # By language breakdown
    print(f"{'Extension':<10} {'Files':>8} {'Code':>12} {'Comment':>12} {'Blank':>10} {'Total':>12}")
    print("-" * 70)

    for ext in sorted(by_ext.keys(), key=lambda e: by_ext[e]['code'], reverse=True):
        stats = by_ext[ext]
        print(f"{ext:<10} {stats['files']:>8} {format_number(stats['code']):>12} "
              f"{format_number(stats['comment']):>12} {format_number(stats['blank']):>10} "
              f"{format_number(stats['total']):>12}")

    print("-" * 70)
    print(f"{'TOTAL':<10} {file_count:>8} {format_number(totals['code']):>12} "
          f"{format_number(totals['comment']):>12} {format_number(totals['blank']):>10} "
          f"{format_number(totals['total']):>12}")
    print()

    # Summary stats
    if totals['total'] > 0:
        code_pct = (totals['code'] / totals['total']) * 100
        comment_pct = (totals['comment'] / totals['total']) * 100
        blank_pct = (totals['blank'] / totals['total']) * 100

        print(f"  Code:    {format_number(totals['code']):>12}  ({code_pct:.1f}%)")
        print(f"  Comment: {format_number(totals['comment']):>12}  ({comment_pct:.1f}%)")
        print(f"  Blank:   {format_number(totals['blank']):>12}  ({blank_pct:.1f}%)")
        print()

    if skipped:
        print(f"  ({skipped} file(s) could not be read)")
        print()


if __name__ == '__main__':
    main()
