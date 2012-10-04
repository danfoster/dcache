/*
COPYRIGHT STATUS:
Dec 1st 2001, Fermi National Accelerator Laboratory (FNAL) documents and
software are sponsored by the U.S. Department of Energy under Contract No.
DE-AC02-76CH03000. Therefore, the U.S. Government retains a  world-wide
non-exclusive, royalty-free license to publish or reproduce these documents
and software for U.S. Government purposes.  All documents and software
available from this server are protected under the U.S. and Foreign
Copyright Laws, and FNAL reserves all rights.

Distribution of the software available from this server is free of
charge subject to the user following the terms of the Fermitools
Software Legal Information.

Redistribution and/or modification of the software shall be accompanied
by the Fermitools Software Legal Information  (including the copyright
notice).

The user is asked to feed back problems, benefits, and/or suggestions
about the software to the Fermilab Software Providers.

Neither the name of Fermilab, the  URA, nor the names of the contributors
may be used to endorse or promote products derived from this software
without specific prior written permission.

DISCLAIMER OF LIABILITY (BSD):

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED  WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FERMILAB,
OR THE URA, OR THE U.S. DEPARTMENT of ENERGY, OR CONTRIBUTORS BE LIABLE
FOR  ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
OF SUBSTITUTE  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY  OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT  OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE  POSSIBILITY OF SUCH DAMAGE.

Liabilities of the Government:

This software is provided by URA, independent from its Prime Contract
with the U.S. Department of Energy. URA is acting independently from
the Government and in its own private capacity and is not acting on
behalf of the U.S. Government, nor as its contractor nor its agent.
Correspondingly, it is understood and agreed that the U.S. Government
has no connection to this software and in no manner whatsoever shall
be liable for nor assume any responsibility or obligation for any claim,
cost, or damages arising out of or resulting from the use of the software
available from this server.

Export Control:

All documents and software available from this server are subject to U.S.
export control laws.  Anyone downloading information from this server is
obligated to secure any necessary Government licenses before exporting
documents or software obtained from this server.
 */
package org.dcache.alarms.dao.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import javax.jdo.FetchPlan;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.dcache.alarms.dao.AlarmEntry;
import org.dcache.alarms.dao.AlarmStorageException;
import org.dcache.alarms.dao.IAlarmLoggingDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataNucleus wrapper to underlying alarm store.<br>
 * <br>
 * Supports the logging appender.
 *
 * @author arossi
 */
public class DataNucleusAlarmStore implements IAlarmLoggingDAO {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private PersistenceManagerFactory pmf;

    /**
     * path to the default implementation (XML) alarm file.
     */
    private final String xmlPath;

    /**
     * for optional overriding of the internal properties resource
     */
    private final String propertiesPath;

    public DataNucleusAlarmStore(String xmlPath, String propertiesPath)
                    throws AlarmStorageException {
        this.xmlPath = xmlPath;
        this.propertiesPath = propertiesPath;
        initialize();
    }

    @Override
    public void put(AlarmEntry alarm) throws AlarmStorageException {
        PersistenceManager insertManager = pmf.getPersistenceManager();
        Transaction tx = insertManager.currentTransaction();
        Query query = insertManager.newQuery(AlarmEntry.class);
        query.setFilter("key==k");
        query.declareParameters("java.lang.String k");
        query.addExtension("datanucleus.query.resultCacheType", "none");
        query.getFetchPlan().setFetchSize(FetchPlan.FETCH_SIZE_OPTIMAL);

        try {
            tx.begin();
            Collection<AlarmEntry> dup =
                            (Collection<AlarmEntry>)query.executeWithArray
                                (new Object[] { alarm.getKey() });
            logger.trace("duplicate? {}", dup);
            if (dup != null && !dup.isEmpty()) {
                if (dup.size() > 1) {
                    throw new AlarmStorageException("data store inconsistency!"
                                    + " more than one alarm with the same id: "
                                    + alarm.getKey());
                }
                AlarmEntry original = dup.iterator().next();
                original.incrementCount();
                /*
                 * original is not detached so it will be updated on commit
                 */
            } else {
                /*
                 * first instance of this alarm
                 */
                logger.trace("makePersistent alarm, key={}", alarm.getKey());
                insertManager.makePersistent(alarm);
                logger.trace("committing");
            }
            tx.commit();
            logger.debug("finished putting alarm, key={}", alarm.getKey());
        } catch (Throwable t) {
            if (tx.isActive()) {
                tx.rollback();
            }
            String message = "committing alarm, key=" + alarm.getKey();
            throw new AlarmStorageException(message, t);
        } finally {
            /*
             * closing is necessary in order to avoid memory leaks
             */
            insertManager.close();
        }
    }

    private void initialize() throws AlarmStorageException {
        try {
            if (propertiesPath != null && !"".equals(propertiesPath.trim())) {
                File file = new File(propertiesPath);
                if (!file.exists()) {
                    throw new FileNotFoundException("Cannot initialize "
                                    + this.getClass()
                                    + " for properties file: " + file);
                }
                pmf = JDOHelper.getPersistenceManagerFactory(file);
            } else {
                Properties properties = new Properties();
                properties.put("javax.jdo.PersistenceManagerFactoryClass",
                                "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
                properties.put("datanucleus.ConnectionURL", "xml:file:" + xmlPath);
                pmf = JDOHelper.getPersistenceManagerFactory(properties);
            }
        } catch (IOException t) {
            throw new AlarmStorageException(t);
        }
    }
}
