package org.genepattern.server.plugin.dao;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.plugin.PatchInfo;
import org.hibernate.Query;

public class PatchInfoDao {
    private static final Logger log = Logger.getLogger(PatchInfoDao.class);
    
    /**
     * Get the entry from the patch_info table for the given patchLsid. 
     * 
     * @param patchLsid
     * @return null if there is no matching entry in the table.
     * @throws Exception
     */
    protected PatchInfo selectPatchInfoByLsid(final String patchLsid) throws DbException {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            final String hql = "from " + PatchInfo.class.getName() + " pi where pi.lsid = :lsid";
            final Query query = HibernateUtil.getSession().createQuery(hql);
            query.setString("lsid", patchLsid);
            List<PatchInfo> list = query.list();
            if (list==null || list.isEmpty()) {
                log.debug("no entry in table for patchLsid="+patchLsid);
                return null;
            }
            if (list.size()>1) {
                log.error("More than one entry in patch_info table with lsid="+patchLsid);
            }
            return list.get(0);
        }
        catch (Throwable t) {
            log.error("Unexpected error getting PatchInfo for lsid="+patchLsid, t);
            throw new DbException("Unexpected error getting PatchInfo for lsid="+patchLsid, t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    public List<PatchInfo> getInstalledPatches() throws DbException {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            String hql = "from " + PatchInfo.class.getName() + " pi";
            Query query = HibernateUtil.getSession().createQuery(hql);
            List<PatchInfo> rval = query.list();
            return rval;
        }
        catch (Throwable t) {
            log.error("Unexpected error selecting installed patches from database: "+t.getLocalizedMessage(), t);
            throw new DbException("Unexpected error selecting installed patches from database: "+t.getLocalizedMessage(), t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public void recordPatch(final PatchInfo patchInfo) throws IllegalArgumentException, DbException {
        if (patchInfo==null) {
            throw new IllegalArgumentException("patchInfo==null");
        }
        if (patchInfo.getLsid()==null) {
            throw new IllegalArgumentException("patchInfo.lsid==null");
        }
        if (patchInfo.getPatchLsid()==null) {
            throw new IllegalArgumentException("patchInfo.patchLsid==null");
        }
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            PatchInfo existing=selectPatchInfoByLsid(patchInfo.getLsid());
            if (existing!=null) {
                patchInfo.setId(existing.getId());
                // need to evict to avoid problems with implementation of hashCode and equals in the PatchInfo class
                HibernateUtil.getSession().evict(existing);
            } 
            HibernateUtil.getSession().saveOrUpdate(patchInfo);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error(t);
            throw new DbException("Unexpected error saving patchInfo to database, patchLsid="+patchInfo.getLsid(), t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public boolean removePatch(PatchInfo patchInfo) throws IllegalArgumentException, DbException {
        if (patchInfo==null) {
            throw new IllegalArgumentException("patchInfo==null");
        }
        if (patchInfo.getLsid()==null) {
            throw new IllegalArgumentException("patchInfo.lsid==null");
        }
        if (patchInfo.getPatchLsid()==null) {
            throw new IllegalArgumentException("patchInfo.patchLsid==null");
        }
        return removePatchByLsid(patchInfo.getLsid());
    }
    
    protected boolean removePatchByLsid(final String patchLsid) throws DbException {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            final String hql = "delete from " + PatchInfo.class.getName() + " pi where pi.lsid = :lsid";
            final Query query = HibernateUtil.getSession().createQuery(hql);
            query.setString("lsid", patchLsid);
            int numDeleted=query.executeUpdate();
            log.debug("numDeleted="+numDeleted);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
            if (numDeleted==1) {
                return true;
            }
            return false;
        }
        catch (Throwable t) {
            final String message="Unexpected error deleting record from patch_info table for lsid="+patchLsid;
            log.error(message, t);
            throw new DbException(message, t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}
