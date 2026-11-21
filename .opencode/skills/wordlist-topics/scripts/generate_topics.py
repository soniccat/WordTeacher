#!/usr/bin/env python3
"""Split a wordlist into topical dictionary groups.

Reads an input wordlist (one word or phrase per line, optional leading
"version:" header), removes entries found in the given exclusion wordlists
(wordlists that are already built, e.g. a core dictionary and a phrase
dictionary), classifies the rest by topic using keyword rules, and writes:

  topic_<name>.wordlist   topical dictionaries (merged rule buckets)
  general_<a>_<z>.wordlist  alphabetical buckets for everything unclassified

Each output file is kept between --min-kb and --max KB (topics that come out
too large are split alphabetically into <name>_2, <name>_3, ...; topics that
come out too small are kept but flagged in the report).

Every input entry ends up in exactly one place: an exclude list or exactly one
output file. This is verified after writing.

Usage:
  python3 generate_topics.py INPUT.wordlist [--exclude A.wordlist B.wordlist ...]
                              [--outdir DIR] [--min-kb 150] [--max-kb 400]
                              [--general-kb N] [--config topics.json]
                              [--sample N] [--list-buckets] [--write]

Without --write the script only prints a report (dry run).
"""
import argparse
import datetime
import json
import os
import random
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from topic_keywords import (
    GEO_PAT, CHEM_KW, CHEM_SUF, MED_KW, ASTRO_KW, BIO_ANIMAL_LAST, ANIMAL_KW,
    PLANT_KW, FOOD_KW, SPORT_KW, MUSIC_KW, RELIG_KW, LAW_KW, FIN_KW, WAR_KW,
    TECH_KW, ART_KW, HIST_KW, POL_KW, LANG_KW, CLOTH_KW, TRANSPORT_KW,
    BUILD_KW, SCIENCE_KW, MATH_KW,
)
from build_groups_local import (
    HOUSE_KW, BODY_KW, EMOTION_KW, NATURE_KW, TIME_KW, WORK_KW, EDU_KW, CRIME_KW,
)

BIRD_KW = [w for w in BIO_ANIMAL_LAST if w.strip() and 'hummingbird' not in w]

# Rule buckets in match-priority order: first matching rule wins, so an entry
# is never placed in two buckets.
RULE_BUCKETS = [
    'chemistry', 'math', 'medicine', 'animals', 'plants', 'food', 'sports',
    'music', 'art', 'astronomy', 'tech', 'war', 'religion', 'law', 'crime',
    'finance', 'politics', 'history', 'education', 'work', 'language',
    'transport', 'buildings', 'household', 'clothing', 'geography', 'nature',
    'science', 'body', 'emotion', 'time',
]

# Default macro groups: which rule buckets are merged into which output file.
DEFAULT_MACRO_GROUPS = {
    'topic_science_tech': ['chemistry', 'math', 'science', 'astronomy', 'tech'],
    'topic_health_body': ['medicine', 'body', 'emotion'],
    'topic_animals': ['animals'],
    'topic_nature_geography': ['plants', 'nature', 'geography'],
    'topic_history_society': ['history', 'war', 'religion', 'politics', 'law',
                              'crime', 'finance', 'work', 'education'],
    'topic_food_home': ['food', 'household', 'buildings', 'transport', 'time'],
    'topic_arts_entertainment': ['sports', 'music', 'art', 'language', 'clothing'],
}

TOPIC_DESCRIPTIONS = {
    'topic_science_tech': 'chemistry, mathematics, physics/general science, astronomy, computing & technology',
    'topic_health_body': 'medicine, diseases, anatomy/body parts, emotions & mental states',
    'topic_animals': 'animals (mammals, birds, fish, insects...) and animal-related compounds/idioms',
    'topic_nature_geography': 'plants, weather/natural phenomena, places & physical geography',
    'topic_history_society': 'history, war/military, religion, politics, law, crime, finance/business, work & education',
    'topic_food_home': 'food & cooking, household objects, buildings/architecture, transport & calendar/daily-time words',
    'topic_arts_entertainment': 'sports & games, music, art, language/literature & clothing/fashion',
}


def kw_re(kws):
    """Compile a word-boundary, case-insensitive regex from keyword phrases."""
    parts = []
    for k in kws:
        k = k.strip()
        if not k:
            continue
        esc = re.escape(k).replace(r'\ ', r'\s+')
        parts.append(r'\b' + esc + r'\b')
    return re.compile('|'.join(parts), re.I)


def build_rules():
    geo = [re.compile(p, re.I) for p in GEO_PAT]
    re_chem = kw_re(CHEM_KW)
    rules = [
        ('chemistry', lambda w: re_chem.search(w) or w.lower().endswith(tuple(CHEM_SUF))),
        ('math', kw_re(MATH_KW)),
        ('medicine', kw_re(MED_KW)),
        ('animals', kw_re(BIRD_KW + ANIMAL_KW)),
        ('plants', kw_re(PLANT_KW)),
        ('food', kw_re(FOOD_KW)),
        ('sports', kw_re(SPORT_KW)),
        ('music', kw_re(MUSIC_KW)),
        ('art', kw_re(ART_KW)),
        ('astronomy', kw_re(ASTRO_KW)),
        ('tech', kw_re(TECH_KW)),
        ('war', kw_re(WAR_KW)),
        ('religion', kw_re(RELIG_KW)),
        ('law', kw_re(LAW_KW)),
        ('crime', kw_re(CRIME_KW)),
        ('finance', kw_re(FIN_KW)),
        ('politics', kw_re(POL_KW)),
        ('history', kw_re(HIST_KW)),
        ('education', kw_re(EDU_KW)),
        ('work', kw_re(WORK_KW)),
        ('language', kw_re(LANG_KW)),
        ('transport', kw_re(TRANSPORT_KW)),
        ('buildings', kw_re(BUILD_KW)),
        ('household', kw_re(HOUSE_KW)),
        ('clothing', kw_re(CLOTH_KW)),
        ('geography', lambda w: any(p.search(w) for p in geo)),
        ('nature', kw_re(NATURE_KW)),
        ('science', kw_re(SCIENCE_KW)),
        ('body', kw_re(BODY_KW)),
        ('emotion', kw_re(EMOTION_KW)),
        ('time', kw_re(TIME_KW)),
    ]
    compiled = []
    for name, fn in rules:
        if hasattr(fn, 'search'):
            compiled.append((name, (lambda rx: lambda w: rx.search(w))(fn)))
        else:
            compiled.append((name, fn))
    return compiled


def load_wordlist(path):
    """Load a wordlist: one entry per line; skip a leading 'version:' header."""
    if not os.path.isfile(path):
        sys.exit(f'error: file not found: {path}')
    with open(path, encoding='utf-8') as f:
        lines = [ln.rstrip('\n') for ln in f]
    if lines and lines[0].startswith('version:'):
        lines = lines[1:]
    return [ln for ln in lines if ln]


def nbytes(words):
    return sum(len(w.encode('utf-8')) + 1 for w in words)


def sort_key(s):
    """Deterministic ordering: case-insensitive, ties broken by raw string."""
    return (s.lower(), s)


def classify(rest):
    """Assign each entry to the first rule bucket that matches (or 'general')."""
    rules = build_rules()
    buckets = {t: [] for t in RULE_BUCKETS}
    buckets['general'] = []
    for w in sorted(rest, key=sort_key):
        for topic, fn in rules:
            if fn(w):
                buckets[topic].append(w)
                break
        else:
            buckets['general'].append(w)
    return buckets


def split_alpha_chunks(words, target_bytes, max_bytes, min_bytes):
    """Split an alphabetically sorted list into chunks of ~target_bytes.

    A trailing chunk that would be smaller than 90% of min_bytes is merged
    into the previous chunk when the result still fits max_bytes.
    """
    chunks, cur, cur_sz = [], [], 0
    for w in words:
        sz = len(w.encode('utf-8')) + 1
        if cur and cur_sz + sz > target_bytes:
            chunks.append(cur)
            cur, cur_sz = [], 0
        cur.append(w)
        cur_sz += sz
    if cur:
        chunks.append(cur)
    if len(chunks) > 1 and nbytes(chunks[-1]) < min_bytes * 0.9:
        merged = chunks[-2] + chunks[-1]
        if nbytes(merged) <= max_bytes:
            chunks = chunks[:-2] + [merged]
    return chunks


def first_letter(w):
    for ch in w.lower():
        if ch.isalpha():
            return ch
    return '0'


def load_macro_groups(config_path):
    """Return {file_base_name: [rule buckets]} from --config or the defaults."""
    if not config_path:
        return dict(DEFAULT_MACRO_GROUPS)
    with open(config_path, encoding='utf-8') as f:
        cfg = json.load(f)
    if not isinstance(cfg, dict) or not cfg:
        sys.exit('error: --config must be a JSON object of {"file-name": ["bucket", ...]}')
    groups = {}
    for name, buckets in cfg.items():
        if isinstance(buckets, str):
            buckets = [buckets]
        if not isinstance(buckets, list) or not buckets:
            sys.exit(f'error: config entry {name!r} must list at least one bucket')
        groups[name] = list(buckets)
    known = set(RULE_BUCKETS)
    for name, buckets in groups.items():
        unknown = [b for b in buckets if b not in known]
        if unknown:
            sys.exit(f'error: config entry {name!r} uses unknown buckets {unknown}; '
                     f'run with --list-buckets for valid names')
    used = [b for buckets in groups.values() for b in buckets]
    if len(used) != len(set(used)):
        sys.exit('error: a rule bucket is used in more than one config entry; '
                 'each bucket may feed only one output file')
    return groups


def assemble(buckets, macro_groups, min_b, max_b, gen_target):
    """Build the final list of (filename, words, description) output files."""
    outputs = []
    notes = []
    for base, parts in macro_groups.items():
        ws = sorted((w for p in parts for w in buckets[p]), key=sort_key)
        if not ws:
            continue
        size = nbytes(ws)
        if size > max_b:
            chunks = split_alpha_chunks(ws, max_b, max_b, min_b)
            for i, ch in enumerate(chunks):
                name = base if i == 0 else f'{base}_{i + 1}'
                outputs.append((f'{name}.wordlist', ch,
                                TOPIC_DESCRIPTIONS.get(base, f'keyword buckets: {", ".join(parts)}')
                                + f' (part {i + 1}, alphabetical split)'))
            notes.append(f'{base} was {size / 1024:.0f} KB -> split into {len(chunks)} alphabetical parts')
        else:
            desc = TOPIC_DESCRIPTIONS.get(base, f'keyword buckets: {", ".join(parts)}')
            outputs.append((f'{base}.wordlist', ws, desc))
            if size < min_b:
                notes.append(f'{base}.wordlist is {size / 1024:.0f} KB, below the '
                             f'{min_b / 1024:.0f} KB minimum (consider merging buckets via --config)')
    gen = buckets['general']
    if gen:
        for ch in split_alpha_chunks(gen, gen_target, max_b, min_b):
            a, z = first_letter(ch[0]), first_letter(ch[-1])
            name = f'general_{a}' if a == z else f'general_{a}_{z}'
            outputs.append((f'{name}.wordlist', ch,
                            'general vocabulary without a clear topic: abstract/rare '
                            'words, idioms, foreign phrases, names'))
    return outputs, notes


def validate(input_set, exclude_set, outdir, filenames, expected_count):
    """Re-read written files and verify the partition invariants."""
    seen = {}
    total = 0
    for fn in filenames:
        for w in load_wordlist(os.path.join(outdir, fn)):
            if w in seen:
                print(f'VALIDATION FAILED: {w!r} appears in both {seen[w]} and {fn}')
                return False
            seen[w] = fn
            total += 1
    if seen.keys() & exclude_set:
        print('VALIDATION FAILED: output overlaps an exclusion wordlist')
        return False
    if set(seen) - input_set:
        print('VALIDATION FAILED: output contains entries not present in the input')
        return False
    if total != expected_count:
        print(f'VALIDATION FAILED: covered {total} entries, expected {expected_count}')
        return False
    return True


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('input', nargs='?', help='input wordlist (one word or phrase per line)')
    ap.add_argument('-e', '--exclude', nargs='*', default=[], metavar='WORDLIST',
                    help='wordlists already built; their entries are not reused in topics')
    ap.add_argument('-o', '--outdir', default=None,
                    help='output directory (default: directory of the input file)')
    ap.add_argument('--min-kb', type=float, default=150,
                    help='soft minimum file size in KB (default: 150)')
    ap.add_argument('--max-kb', type=float, default=400,
                    help='maximum file size in KB (default: 400)')
    ap.add_argument('--general-kb', type=float, default=None,
                    help='target size of general_* buckets in KB (default: derived)')
    ap.add_argument('--config', default=None, metavar='JSON',
                    help='JSON file mapping output names to rule buckets (custom layout)')
    ap.add_argument('--sample', type=int, default=5,
                    help='random samples shown per file in the report (default: 5)')
    ap.add_argument('--list-buckets', action='store_true',
                    help='print the available rule bucket names and exit')
    ap.add_argument('--write', action='store_true',
                    help='write output files (without it, only print a report)')
    args = ap.parse_args()

    if args.list_buckets:
        print('Available rule buckets (match priority order):')
        for b in RULE_BUCKETS:
            print(' ', b)
        return

    if not args.input:
        ap.error('the input wordlist is required unless --list-buckets is used')

    min_b = args.min_kb * 1024
    max_b = args.max_kb * 1024
    gen_target = (args.general_kb or min(290, args.max_kb * 0.85)) * 1024

    input_lines = load_wordlist(args.input)
    input_set = set(input_lines)
    if len(input_lines) != len(input_set):
        print(f'note: input has {len(input_lines) - len(input_set)} duplicate lines (deduplicated)')

    exclude_set = set()
    for path in args.exclude:
        excl = set(load_wordlist(path))
        hits = len(input_set & excl)
        print(f'exclude {os.path.basename(path)}: {len(excl)} entries, {hits} matched in input')
        exclude_set |= excl
    rest = input_set - exclude_set
    if not rest:
        sys.exit('error: nothing left to classify after exclusions')

    buckets = classify(rest)
    macro_groups = load_macro_groups(args.config)
    outputs, notes = assemble(buckets, macro_groups, min_b, max_b, gen_target)

    rng = random.Random(0)
    print(f'\n{"file":34s} {"entries":>8s} {"size":>9s}')
    for fn, ws, _ in outputs:
        print(f'{fn:34s} {len(ws):8d} {nbytes(ws) / 1024:8.1f} KB')
    print(f'{"total":34s} {sum(len(ws) for _, ws, _ in outputs):8d} '
          f'{sum(nbytes(ws) for _, ws, _ in outputs) / 1024:8.1f} KB')
    if args.sample > 0:
        print(f'\nsamples (seeded, reproducible):')
        for fn, ws, _ in outputs:
            print(f'  {fn}: ' + ' | '.join(rng.sample(ws, min(args.sample, len(ws)))))
    for n in notes:
        print('note: ' + n)

    assigned = sum(len(ws) for _, ws, _ in outputs)
    assert assigned == len(rest), f'internal error: partition broken ({assigned} != {len(rest)})'

    if not args.write:
        print('\ndry run only; re-run with --write to create the files')
        return

    outdir = args.outdir or os.path.dirname(os.path.abspath(args.input))
    os.makedirs(outdir, exist_ok=True)
    for old in os.listdir(outdir):
        if old.startswith(('topic_', 'general_')) and old.endswith('.wordlist'):
            os.remove(os.path.join(outdir, old))
    for fn, ws, _ in outputs:
        with open(os.path.join(outdir, fn), 'w', encoding='utf-8') as f:
            f.write('\n'.join(ws) + '\n')

    ok = validate(input_set, exclude_set, outdir, [fn for fn, _, _ in outputs], len(rest))

    with open(os.path.join(outdir, 'groups_report.txt'), 'w', encoding='utf-8') as f:
        f.write(f'Topical wordlist groups generated from {args.input}\n')
        if args.exclude:
            f.write(f'excluding entries from: {", ".join(args.exclude)}\n')
        f.write(f'generated: {datetime.datetime.now().isoformat(timespec="seconds")}\n\n')
        for fn, ws, desc in outputs:
            f.write(f'{fn:34s} {len(ws):7d} entries  {nbytes(ws) / 1024:7.1f} KB\n    {desc}\n')
        f.write(f'\nTotal: {sum(nbytes(ws) for _, ws, _ in outputs) / 1024:.0f} KB, '
                f'{sum(len(ws) for _, ws, _ in outputs)} entries across {len(outputs)} files\n')
        f.write('Every input entry is in exactly one place: an exclude wordlist or one group file.\n')
        for n in notes:
            f.write(f'note: {n}\n')

    print(f'\nwrote {len(outputs)} files + groups_report.txt to {outdir}')
    if not ok:
        sys.exit(1)
    print('validation PASSED: no duplicates, no overlap with excludes, full coverage')


if __name__ == '__main__':
    main()
