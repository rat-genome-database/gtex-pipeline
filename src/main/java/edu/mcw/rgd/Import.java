package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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

    NumberFormat plusMinusNF = new DecimalFormat(" +###,###,###; -###,###,###");

    public static void main(String[] args) throws Exception {

        test();
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

        int initialGtexCount = dao.getCountOfGtexIds(getSourcePipeline());

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
            dao.insertXdbs(idsToBeInserted);
            log.info("inserted xdb ids for GTEx: "+idsToBeInserted.size());
        }

        if( !idsToBeDeleted.isEmpty() ) {
            dao.deleteXdbIds(idsToBeDeleted);
            log.info("deleted xdb ids for GTEx:  "+idsToBeDeleted.size());
        }

        if( !idsMatching.isEmpty() ) {
            dao.updateModificationDate(idsMatching);
            log.info("last-modified-date updated for GTEx ids: "+idsMatching.size());
        }

        int finalGtexCount = dao.getCountOfGtexIds(getSourcePipeline());
        int diffCount = finalGtexCount - initialGtexCount;
        String diffCountStr = diffCount!=0 ? "     difference: "+ plusMinusNF.format(diffCount) : "     no changes";
        log.info("final GTEx IDs count: "+Utils.formatThousands(finalGtexCount)+diffCountStr);

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

        log.info("incoming GTEx IDs via Ensembl Gene id: "+gtexIdsByEnsemblGeneId);
        log.info("incoming GTEx IDs via gene symbol: "+gtexIdsByGeneSymbol);

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

    public static void test() throws IOException {

        String[] forbidden = {
                "\nST = T001\n",
                "\nST = T004\n",
                "\nST = T005\n",
                "\nST = T007\n",
                "\nST = T015\n",
                "\nST = T103\n",
                "\nST = T104\n",
                "\nST = T109\n",
                "\nST = T114\n",
                "\nST = T116\n",
                "\nST = T121\n",
                "\nST = T122\n",
                "\nST = T123\n",
                "\nST = T125\n",
                "\nST = T126\n",
                "\nST = T127\n",
                "\nST = T129\n",
                "\nST = T130\n",
                "\nST = T131\n",
                "\nST = T192\n",
                "\nST = T194\n",
                "\nST = T195\n",
                "\nST = T196\n",
                "\nST = T197\n",
        };
        String fnameIn = "/tmp/c2021.bin";
        String fnameOut = "/tmp/c2021.bin.txt";
        BufferedReader in = Utils.openReader(fnameIn);
        BufferedWriter out = new BufferedWriter(new FileWriter(fnameOut));

        int records = 0;
        int skipped = 0;
        StringBuffer rec = new StringBuffer();
        String line;
        while( (line=in.readLine())!=null ) {
            if( line.startsWith("*NEWRECORD") ) {
                // flush previous record
                String s = rec.toString();
                boolean isForbidden = false;
                if( !s.contains("\nST = ") ) {
                    isForbidden = true;
                } else {
                    for (String forbiddenST : forbidden) {
                        if (s.contains(forbiddenST)) {
                            isForbidden = true;
                            break;
                        }
                    }
                }
                if( !isForbidden ) {
                    out.write(s);
                } else {
                    skipped ++;
                }
                records++;
                rec = new StringBuffer();
            }
            rec.append(line).append("\n");
        }

        String s = rec.toString();
        boolean isForbidden = false;
        for( String forbiddenST: forbidden ) {
            if( s.contains(forbiddenST) ) {
                isForbidden = true;
                break;
            }
        }
        if( !isForbidden ) {
            out.write(s);
        } else {
            skipped ++;
        }

        in.close();
        out.close();

        System.out.println("records: "+records+", skipped= "+skipped);

        System.exit(0);
    }
}

