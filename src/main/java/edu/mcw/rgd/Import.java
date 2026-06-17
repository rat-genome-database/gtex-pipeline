package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.MemoryMonitor;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map;

/**
 * @author mtutaj
 * @since 11/11/2017
 */
public class Import {

    private final DAO dao = new DAO();
    private String version;
    private String sourcePipeline;

    Logger log = LogManager.getLogger("status");

    NumberFormat plusMinusNF = new DecimalFormat(" +###,###,###; -###,###,###");

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

        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.start();

        log.info(getVersion());
        log.info("   "+dao.getConnectionInfo());
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date(time0)));

        boolean ok = false;
        try {
            int initialGtexCount = dao.getCountOfGtexIds(getSourcePipeline());


            log.debug("QC: get GTEx Ids in RGD");
            List<XdbId> idsInRgd = dao.getGTExXdbIds(getSourcePipeline());
            log.debug("QC: get incoming GTEx Ids");
            List<XdbId> idsIncoming = getIncomingIds();


            // determine xdb ids for insertion, deletion and matching

            // determine to-be-inserted ids
            log.debug("QC: determine to-be-inserted Ids");
            List<XdbId> idsToBeInserted = new ArrayList<>(CollectionUtils.subtract(idsIncoming, idsInRgd));

            // determine matching ids
            log.debug("QC: determine matching Ids");
            List<XdbId> idsMatching = new ArrayList<>(CollectionUtils.intersection(idsInRgd, idsIncoming));

            // determine to-be-deleted ids
            log.debug("QC: determine to-be-deleted Ids");
            List<XdbId> idsToBeDeleted = new ArrayList<>(CollectionUtils.subtract(idsInRgd, idsIncoming));


            // loading
            if( !idsToBeInserted.isEmpty() ) {
                dao.insertXdbs(idsToBeInserted);
                log.info("inserted xdb ids for GTEx: "+Utils.formatThousands(idsToBeInserted.size()));
            }

            if( !idsToBeDeleted.isEmpty() ) {
                dao.deleteXdbIds(idsToBeDeleted);
                log.info("deleted xdb ids for GTEx:  "+Utils.formatThousands(idsToBeDeleted.size()));
            }

            if( !idsMatching.isEmpty() ) {
                dao.updateModificationDate(idsMatching);
                log.info("last-modified-date updated for GTEx ids: "+Utils.formatThousands(idsMatching.size()));
            }

            int finalGtexCount = dao.getCountOfGtexIds(getSourcePipeline());
            int diffCount = finalGtexCount - initialGtexCount;
            String diffCountStr = diffCount!=0 ? "     difference: "+ plusMinusNF.format(diffCount) : "     no changes";
            log.info("final GTEx IDs count: "+Utils.formatThousands(finalGtexCount)+diffCountStr);

            ok = true;
        } finally {
            memoryMonitor.stop();
            log.info(memoryMonitor.getSummary());
            log.info((ok ? "GTEx ID generation complete -- " : "GTEx ID generation FAILED -- ") + "time elapsed: " + Utils.formatElapsedTime(time0, System.currentTimeMillis()));
            log.info("====");
        }
    }

    List<XdbId> getIncomingIds() throws Exception {

        int gtexIdsByGeneSymbol = 0;
        int gtexIdsByEnsemblGeneId = 0;
        Date dt = new Date();

        // bulk-load all Ensembl gene ids for human genes once, grouped by rgd id,
        // instead of issuing one query per gene
        Map<Integer, Set<String>> ensemblIdsByRgdId = new HashMap<>();
        for( XdbId x: dao.getXdbIds(XdbId.XDB_KEY_ENSEMBL_GENES, SpeciesType.HUMAN) ) {
            ensemblIdsByRgdId.computeIfAbsent(x.getRgdId(), k -> new HashSet<>()).add(x.getAccId());
        }

        List<Gene> genes = dao.getActiveGenes(SpeciesType.HUMAN);
        List<XdbId> incomingIds = new ArrayList<>(genes.size());
        for (Gene g: genes) {

            Set<String> ids = ensemblIdsByRgdId.getOrDefault(g.getRgdId(), Collections.emptySet());

            if( ids.isEmpty() ) {
                // create GTEx link via gene symbol
                incomingIds.add(newGtexXdbId(g.getRgdId(), g.getSymbol(), dt));
                gtexIdsByGeneSymbol++;
            } else {
                // create GTEx link via Ensembl Gene ID(s)
                for( String ensemblGeneId: ids ) {
                    incomingIds.add(newGtexXdbId(g.getRgdId(), ensemblGeneId, dt));
                    gtexIdsByEnsemblGeneId++;
                }
            }
        }

        log.info("incoming GTEx IDs via Ensembl Gene id: "+Utils.formatThousands(gtexIdsByEnsemblGeneId));
        log.info("incoming GTEx IDs via gene symbol    : "+Utils.formatThousands(gtexIdsByGeneSymbol));

        return incomingIds;
    }

    private XdbId newGtexXdbId(int rgdId, String accId, Date dt) {
        XdbId x = new XdbId();
        x.setAccId(accId);
        x.setSrcPipeline(getSourcePipeline());
        x.setRgdId(rgdId);
        x.setXdbKey(DAO.XDB_KEY_GTEX);
        x.setCreationDate(dt);
        x.setModificationDate(dt);
        return x;
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

