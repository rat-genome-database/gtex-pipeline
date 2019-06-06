package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author mtutaj
 * @since 11/11/2017
 */
public class Import {

    private DAO dao = new DAO();
    private String version;
    private String sourcePipeline;

    Logger log = Logger.getLogger("status");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Import manager = (Import) (bf.getBean("manager"));

        try {
            manager.run();
        }catch (Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    public void run() throws Exception {

        long time0 = System.currentTimeMillis();

        log.info(getVersion());
        log.info("   "+dao.getConnectionInfo());
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date(time0)));

        // QC
        log.debug("QC: get GTEx Ids in RGD");
        List<XdbId> idsInRgd = dao.getGTExXdbIds(getSourcePipeline());
        log.debug("QC: get incoming GTEx Ids");
        List<XdbId> idsIncoming = getIncomingIds();

        // determine to-be-inserted ids
        log.debug("QC: determine to-be-inserted Ids");
        List<XdbId> idsToBeInserted = new ArrayList<XdbId>(idsIncoming);
        idsToBeInserted.removeAll(idsInRgd);

        // determine matching ids
        log.debug("QC: determine matching Ids");
        List<XdbId> idsMatching = new ArrayList<XdbId>(idsIncoming);
        idsMatching.retainAll(idsInRgd);

        // determine to-be-deleted ids
        log.debug("QC: determine to-be-deleted Ids");
        idsInRgd.removeAll(idsIncoming);
        List<XdbId> idsToBeDeleted = idsInRgd;


        // loading
        if( !idsToBeInserted.isEmpty() ) {
            log.info("inserting xdb ids for GTEx (Human): "+idsToBeInserted.size());
            dao.insertXdbs(idsToBeInserted);
        }

        if( !idsToBeDeleted.isEmpty() ) {
            log.info("deleting xdb ids for GTEx (Human): "+idsToBeDeleted.size());
            dao.deleteXdbIds(idsToBeDeleted);
        }

        if( !idsMatching.isEmpty() ) {
            log.info("matching xdb ids for GTEx (Human): "+idsMatching.size());
            dao.updateModificationDate(idsMatching);
        }

        log.info("GTEx ID generation complete -- time elapsed: "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));
    }

    List<XdbId> getIncomingIds() throws Exception {

        int gtexIdsByGeneSymbol = 0;
        int gtexIdsByEnsemblGeneId = 0;
        Date dt = new Date();

        List<Gene> genes = dao.getActiveGenes(SpeciesType.HUMAN);
        List<XdbId> incomingIds = new ArrayList<XdbId>(genes.size());
        for (Gene g: genes) {

            // load unique set of ensembl gene ids for this gene
            List<XdbId> idsFromDb = dao.getXdbIdsByRgdId(XdbId.XDB_KEY_ENSEMBL_GENES, g.getRgdId());
            Set<String> ids = new HashSet<String>();
            for( XdbId x: idsFromDb ) {
                ids.add(x.getAccId());
            }

            if( ids.isEmpty() ) {
                // create GTEx link via gene symbol
                XdbId x = new XdbId();
                x.setAccId(g.getSymbol());
                x.setSrcPipeline(getSourcePipeline());
                x.setRgdId(g.getRgdId());
                x.setXdbKey(DAO.XDB_KEY_GTEX);
                x.setCreationDate(dt);
                x.setModificationDate(dt);
                incomingIds.add(x);
                gtexIdsByGeneSymbol++;
            } else {
                // create GTEx link via Ensembl Gene ID(s)
                for( String ensemblGeneId: ids ) {
                    XdbId x = new XdbId();
                    x.setAccId(ensemblGeneId);
                    x.setSrcPipeline(getSourcePipeline());
                    x.setRgdId(g.getRgdId());
                    x.setXdbKey(DAO.XDB_KEY_GTEX);
                    x.setCreationDate(dt);
                    x.setModificationDate(dt);
                    incomingIds.add(x);
                    gtexIdsByEnsemblGeneId++;
                }
            }
        }

        log.info("incoming GTEx IDs via gene symbol: "+gtexIdsByGeneSymbol);
        log.info("incoming GTEx IDs via Ensembl Gene id: "+gtexIdsByEnsemblGeneId);

        return incomingIds;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setSourcePipeline(String sourcePipeline) {
        this.sourcePipeline = sourcePipeline;
    }

    public String getSourcePipeline() {
        return sourcePipeline;
    }
}

