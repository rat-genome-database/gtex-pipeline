v 1.0.1, Aug 24, 2018
 -updated logging to log4j

v 1.0, Nov 13, 2017
 -initial version of the pipeline


LOGIC:
  FOR every active human gene
    IF gene has Ensembl gene Ids
	  THEN generate GTex link for every of Ensembl gene ids
	ELSE
	  THEN generate GTex link for gene symbol
	FI
