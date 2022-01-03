# gtex-pipeline
Create GTEx external database ids for human genes, based on Ensembl Gene Ids, and if these are not available, based on gene symbols.

LOGIC:
  FOR every active human gene
    IF gene has Ensembl gene Ids
	  THEN generate GTex link for every of Ensembl gene ids
	ELSE
	  THEN generate GTex link for gene symbol
	FI
