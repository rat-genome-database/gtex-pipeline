# gtex-pipeline

Creates [GTEx Portal](https://gtexportal.org) external database links for active human genes in RGD and keeps them in sync on every run.

## What it does

For every active human gene, the pipeline generates a GTEx xref:

- if the gene has one or more **Ensembl Gene IDs**, a GTEx link is created for each Ensembl ID;
- otherwise, a single GTEx link is created using the **gene symbol** as a fallback.

The generated xrefs are diffed against what's already in RGD for source pipeline `GTEx`:

- **insert** — rows present in the incoming set but missing in RGD
- **update** — rows present in both (last-modified-date bumped)
- **delete** — rows present in RGD but no longer produced by the pipeline

Counts and elapsed time are written to `logs/status.log`, and a run summary is emailed at the end of `run.sh`.
