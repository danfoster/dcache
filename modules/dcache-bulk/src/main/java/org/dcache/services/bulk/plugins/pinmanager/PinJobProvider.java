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
package org.dcache.services.bulk.plugins.pinmanager;

import static org.dcache.services.bulk.job.BulkJobArgumentDescriptor.EMPTY_DEFAULT;
import static org.dcache.services.bulk.job.MultipleTargetJob.TargetType.FILE;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.dcache.services.bulk.job.BulkJobArgumentDescriptor;
import org.dcache.services.bulk.job.BulkJobKey;
import org.dcache.services.bulk.job.BulkJobProvider;
import org.dcache.services.bulk.job.TargetExpansionJob.ExpansionType;

public class PinJobProvider extends BulkJobProvider<PinJob> {

    static final BulkJobArgumentDescriptor LIFETIME
          = new BulkJobArgumentDescriptor("lifetime",
          "duration of the pin",
          "long",
          false,
          "5");

    static final BulkJobArgumentDescriptor LIFETIME_UNIT =
          new BulkJobArgumentDescriptor("lifetime-unit",
                "time unit for duration of the pin",
                "SECONDS|MINUTES|HOURS|DAYS",
                false,
                "MINUTES");

    static final BulkJobArgumentDescriptor PIN_REQUEST_ID
          = new BulkJobArgumentDescriptor("id",
          "to use for this pin.  If empty/null on PIN, the id of the current request will "
                + "be used)",
          "string",
          false,
         EMPTY_DEFAULT);

    public PinJobProvider() {
        super("PIN", FILE, ExpansionType.BREADTH_FIRST);
    }

    @Override
    public PinJob createJob(BulkJobKey key, BulkJobKey parentKey) {
        return new PinJob(key, parentKey, activity);
    }

    @Override
    public Class<PinJob> getJobClass() {
        return PinJob.class;
    }

    @Override
    public Set<BulkJobArgumentDescriptor> getArguments() {
        return ImmutableSet.of(LIFETIME, LIFETIME_UNIT, PIN_REQUEST_ID);
    }
}