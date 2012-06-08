package org.dcache.gplazma.plugins;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.get;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.dcache.auth.GidPrincipal;
import org.dcache.auth.GroupNamePrincipal;
import org.dcache.auth.LoginGidPrincipal;
import org.dcache.auth.LoginNamePrincipal;
import org.dcache.auth.LoginUidPrincipal;
import org.dcache.auth.UidPrincipal;
import org.dcache.auth.UserNamePrincipal;
import org.dcache.auth.attributes.HomeDirectory;
import org.dcache.auth.attributes.ReadOnly;
import org.dcache.auth.attributes.RootDirectory;
import org.dcache.gplazma.AuthenticationException;
import org.dcache.gplazma.plugins.AuthzMapLineParser.UserAuthzInformation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.common.base.Splitter;

import static org.dcache.gplazma.util.Preconditions.checkAuthentication;

/**
 * Plugin uses AuthzDB for mapping group names and user names to UID,
 * GIDs.
 *
 * User names are typically generated by mapping DNs or login names in
 * another plugin.
 *
 * Group names are typically generated by mapping FQANs in another
 * plugin and there will be a primary group name.
 *
 * LoginNamePrincipal, LoginUidPrincipal, and LoginGidPrincipal are
 * used to pick one among multiple possible UIDs and to pick a primary
 * group.
 */
public class AuthzDbPlugin
    implements GPlazmaMappingPlugin, GPlazmaSessionPlugin
{
    private static final long REFRESH_PERIOD =
        TimeUnit.SECONDS.toMillis(10);

    private static final String AUTHZDB =
        "gplazma.authzdb.file";
    private static final String UID =
        "gplazma.authzdb.uid";
    private static final String GID =
        "gplazma.authzdb.gid";

    enum PrincipalType { UID, GID, LOGIN, USER, GROUP };

    private final ImmutableList<PrincipalType> _uidOrder;
    private final ImmutableList<PrincipalType> _gidOrder;

    private final SourceBackedPredicateMap<String,UserAuthzInformation> _map;

    public AuthzDbPlugin(Properties properties) throws IOException
    {
        String path = properties.getProperty(AUTHZDB);
        String uid = properties.getProperty(UID);
        String gid = properties.getProperty(GID);

        checkArgument(path != null, "Undefined property: " + AUTHZDB);
        checkArgument(uid != null, "Undefined property: " + UID);
        checkArgument(gid != null, "Undefined property: " + GID);

        _map = new SourceBackedPredicateMap<String,UserAuthzInformation>(new FileLineSource(path, REFRESH_PERIOD), new AuthzMapLineParser());
        _uidOrder = parseOrder(uid);
        _gidOrder = parseOrder(gid);
    }

    /**
     * package visible constructor for testing purposes
     * @param authzMapCache map of usernames to user information (e.q. uid/gid)
     */
    AuthzDbPlugin(SourceBackedPredicateMap<String,UserAuthzInformation> map,
                  ImmutableList<PrincipalType> uidOrder,
                  ImmutableList<PrincipalType> gidOrder)
    {
        _map = map;
        _uidOrder = uidOrder;
        _gidOrder = gidOrder;
    }

    static ImmutableList<PrincipalType> parseOrder(String s)
    {
        ImmutableList.Builder<PrincipalType> order = ImmutableList.builder();
        for (String e: Splitter.on(',').omitEmptyStrings().split(s)) {
            order.add(PrincipalType.valueOf(e.toUpperCase()));
        }
        return order.build();
    }

    private UserAuthzInformation getEntity(String name)
        throws AuthenticationException
    {
        Collection<UserAuthzInformation> mappings =
            _map.getValuesForPredicatesMatching(name);

        checkAuthentication(!mappings.isEmpty(),
                "no mapping exists for " + name);
        return get(mappings, 0);
    }

    private UserAuthzInformation
        getEntity(List<PrincipalType> order,
                  Long loginUid, Long loginGid, String loginName,
                  String userName, String primaryGroup)
        throws AuthenticationException
    {
        for (PrincipalType type: order) {
            switch (type) {
            case UID:
                if (loginUid != null) {
                    return new UserAuthzInformation(null, null, loginUid, null, null, null, null);
                }
                break;

            case GID:
                if (loginGid != null) {
                    return new UserAuthzInformation(null, null, 0, new long[] { loginGid.longValue() }, null, null, null);
                }
                break;

            case LOGIN:
                if (loginName != null) {
                    return getEntity(loginName);
                }
                break;

            case USER:
                if (userName != null) {
                    return getEntity(userName);
                }
                break;

            case GROUP:
                if (primaryGroup != null) {
                    return getEntity(primaryGroup);
                }
                break;
            }
        }
        throw new AuthenticationException("no mappable principal");
    }

    @Override
    public void map(Set<Principal> principals,
                    Set<Principal> authorizedPrincipals)
        throws AuthenticationException
    {
        /* Classify input principals.
         */
        List<String> names = Lists.newArrayList();
        String loginName = null;
        Long loginUid = null;
        Long loginGid = null;
        String userName = null;
        String primaryGroup = null;
        for (Principal principal: principals) {
            if (principal instanceof LoginNamePrincipal) {
                checkAuthentication(loginName == null, "multiple login names");
                loginName = principal.getName();
            } else if (principal instanceof LoginUidPrincipal) {
                checkAuthentication(loginUid == null, "multiple login UIDs");
                loginUid = ((LoginUidPrincipal) principal).getUid();
            } else if (principal instanceof LoginGidPrincipal) {
                checkAuthentication(loginGid == null, "multiple login GIDs");
                loginGid = ((LoginGidPrincipal) principal).getGid();
            } else if (principal instanceof UserNamePrincipal) {
                checkAuthentication(userName == null, "multiple usernames");
                userName = principal.getName();
                names.add(userName);
            } else if (principal instanceof GroupNamePrincipal) {
                if (((GroupNamePrincipal) principal).isPrimaryGroup()) {
                    checkAuthentication(primaryGroup == null,
                            "multiple primary group names");
                    primaryGroup = principal.getName();
                }
                names.add(principal.getName());
            }
        }

        /* Determine the UIDs and GIDs available to the user
         */
        List<Long> uids = Lists.newArrayList();
        List<Long> gids = Lists.newArrayList();
        for (String name: names) {
            Collection<UserAuthzInformation> mappings =
                _map.getValuesForPredicatesMatching(name);
            for (UserAuthzInformation mapping: mappings) {
                uids.add(mapping.getUid());
                gids.addAll(Longs.asList(mapping.getGids()));
            }
        }

        /* Verify that the login name, login UID and login GID are
         * among the valid values.
         */
        checkAuthentication(loginName == null || names.contains(loginName),
                "not authorized to use login name: " + loginName);
        checkAuthentication(loginUid == null || uids.contains(loginUid),
                "not authorized to use UID: " + loginUid);
        checkAuthentication(loginGid == null || gids.contains(loginGid),
                "not authorized to use GID: " + loginGid);

        /* Pick a UID and user name to authorize.
         */
        UserAuthzInformation user =
            getEntity(_uidOrder, loginUid, null, loginName, userName, primaryGroup);
        authorizedPrincipals.add(new UidPrincipal(user.getUid()));
        if (user.getUsername() != null) {
            authorizedPrincipals.add(new UserNamePrincipal(user.getUsername()));
        }

        /* Pick a GID to authorize.
         */
        UserAuthzInformation group =
            getEntity(_gidOrder, null, loginGid, loginName, userName, primaryGroup);
        long primaryGid = group.getGids()[0];
        authorizedPrincipals.add(new GidPrincipal(primaryGid, true));

        /* Add remaining gids.
         */
        for (long gid: gids) {
            if (gid != primaryGid) {
                authorizedPrincipals.add(new GidPrincipal(gid, false));
            }
        }
    }

    @Override
    public void session(Set<Principal> authorizedPrincipals,
                        Set<Object> attrib)
        throws AuthenticationException
    {
        Principal principal =
            find(authorizedPrincipals, instanceOf(UserNamePrincipal.class), null);
        if (principal != null) {
            Collection<UserAuthzInformation> mappings =
                _map.getValuesForPredicatesMatching(principal.getName());
            for (UserAuthzInformation mapping: mappings) {
                attrib.add(new HomeDirectory(mapping.getHome()));
                attrib.add(new RootDirectory(mapping.getRoot()));
                attrib.add(new ReadOnly(mapping.isReadOnly()));
            }
        }
    }
}
