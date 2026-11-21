---
name: Wordlist Topics
description: Split a wordlist file (one word or phrase per line) into topical dictionary groups (topic_*.wordlist) plus alphabetical general_* buckets, each about 150-400 KB, optionally excluding words already used in other built wordlists
---

# Wordlist Topics

Turn one big wordlist into topic-based dictionary wordlists a user can pick
from. Runs a keyword classifier over every entry and writes one file per
topic, plus alphabetically split `general_*` buckets for entries without a
clear topic. Guarantees: every input entry ends up in exactly one output file,
and entries from the exclusion wordlists are never reused.

## Setup

The generator lives in `scripts/` relative to this skill's directory (the
directory containing this SKILL.md). All examples below assume you invoke it
from that directory or use the absolute path. It only needs Python 3.

## Input format

- Input wordlist: one word or phrase per line, UTF-8. A leading `version:`
  header line is skipped automatically if present.
- Exclusion wordlists: same format. They are only read, never modified.

## Workflow

1. Collect from the user: the input wordlist, and optionally the wordlists
   that are already built (e.g. a core dictionary and a phrase dictionary)
   whose entries must not be reused.
2. Dry run first and read the report:
   ```sh
   python3 scripts/generate_topics.py app.wordlist \
       --exclude core_dsl.wordlist phrases_and_idioms.wordlist
   ```
   The report lists every output file with entry count, size in KB, seeded
   random samples, and any notes.
3. Check sizes. Each file should be about `--min-kb` (150) to `--max-kb` (400).
   - A topic larger than the max is split alphabetically into
     `<name>.wordlist`, `<name>_2.wordlist`, ... automatically.
   - A topic smaller than the min is kept but flagged in a note. Fix it by
     merging rule buckets via `--config` (see below) and re-running.
4. Write the files:
   ```sh
   python3 scripts/generate_topics.py app.wordlist \
       --exclude core_dsl.wordlist phrases_and_idioms.wordlist \
       --write
   ```
   Output goes next to the input file by default; use `--outdir DIR` to change
   this. Pre-existing `topic_*.wordlist` / `general_*.wordlist` files in the
   output directory are replaced. A `groups_report.txt` with all file
   descriptions is written alongside.
5. Confirm the run ended with `validation PASSED`. If validation fails, report
   the failure to the user instead of leaving broken output behind.
6. Summarize the result for the user: file list, sizes, and the report path.

## Custom topic layout

The default layout merges the 31 rule buckets into 7 topic files. To change
which buckets feed which file, write a JSON config and pass `--config`:

```json
{
  "topic_animals": ["animals"],
  "topic_plants_nature": ["plants", "nature", "geography"]
}
```

- Keys become file base names (`topic_plants_nature.wordlist`).
- Values are lists of rule bucket names. Each bucket may feed only one file.
- List the valid bucket names with `python3 scripts/generate_topics.py --list-buckets`.

## Other options

- `--min-kb 150 --max-kb 400` — size window (defaults shown).
- `--general-kb N` — target size of the `general_*` buckets.
- `--sample N` — samples per file in the report (0 disables).
- `--outdir DIR` — where files are written.

## Notes and limitations

- Classification is keyword based (word-boundary, case-insensitive) and tuned
  for general English dictionary content. Expect roughly 10% of the entries in
  a topical file to be only loosely related (e.g. animal-themed idioms land in
  the animals file). Sample a few entries from the report and tell the user
  when a topic looks noisy; keyword lists live in `scripts/topic_keywords.py`
  and `scripts/build_groups_local.py` and can be tuned.
- Rule buckets are checked in a fixed priority order, so an entry is never in
  two buckets; see `RULE_BUCKETS` in `scripts/generate_topics.py`.
- The `version:` header of the input is never copied to the output files.
