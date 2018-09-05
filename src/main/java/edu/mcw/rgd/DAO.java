package edu.mcw.rgd;

import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.dao.impl.XdbIdDAO;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mtutaj
 * @since 11/11/17
 * <p>
 * wrapper to handle all DAO code
 */
public class DAO {

    public static final int XDB_KEY_GTEX = 65;
    XdbIdDAO xdao = new XdbIdDAO();
    GeneDAO gdao = new GeneDAO();

    Logger logInserted = Logger.getLogger("insertedIds");
    Logger logDeleted = Logger.getLogger("deletedIds");

    public String getConnectionInfo() {
        return xdao.getConnectionInfo();
    }

    public List<XdbId> getGTExXdbIds(String srcPipeline) throws Exception {

        XdbId filter = new XdbId();
        filter.setXdbKey(XDB_KEY_GTEX);
        filter.setSrcPipeline(srcPipeline);
        return xdao.getXdbIds(filter, SpeciesType.HUMAN);
    }

    /**
     * return external ids for given xdb key and rgd-id
     * @param xdbKey - external database key (like 2 for PubMed)
     * @param rgdId - rgd-id
     * @return list of external ids
     */
    public List<XdbId> getXdbIdsByRgdId(int xdbKey, int rgdId) throws Exception {
        return xdao.getXdbIdsByRgdId(xdbKey, rgdId);
    }

    /**
     * Returns all active genes for given species. Results do not contain splices or alleles
     * @param speciesKey species type key
     * @return list of active genes for given species
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<Gene> getActiveGenes(int speciesKey) throws Exception {
        return gdao.getActiveGenes(speciesKey);
    }

    /**
     * insert a bunch of XdbIds; duplicate entries are not inserted (with same RGD_ID,XDB_KEY,ACC_ID,SRC_PIPELINE)
     * @param xdbs list of XdbIds objects to be inserted
     * @return number of actually inserted rows
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int insertXdbs(List<XdbId> xdbs) throws Exception {

        for( XdbId xdbId: xdbs ) {
            logInserted.debug(xdbId.dump("|"));
        }

        return xdao.insertXdbs(xdbs);
    }

    /**
     * delete a list external ids (RGD_ACC_XDB rows);
     * if ACC_XDB_KEY is provided, it is used to delete the row;
     * else ACC_ID, RGD_ID, XDB_KEY and SRC_PIPELINE are used to locate and delete every row
     *
     * @param xdbIds list of external ids to be deleted
     * @return nr of rows deleted
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int deleteXdbIds( List<XdbId> xdbIds ) throws Exception {

        for( XdbId xdbId: xdbIds ) {
            logDeleted.debug(xdbId.dump("|"));
        }

        return xdao.deleteXdbIds(xdbIds);
    }

    public int updateModificationDate(List<XdbId> xdbIds) throws Exception {

        List<Integer> xdbKeys = new ArrayList<Integer>(xdbIds.size());
        for( XdbId xdbId: xdbIds ) {
            xdbKeys.add(xdbId.getKey());
        }
        return xdao.updateModificationDate(xdbKeys);
    }
}
