package org.cacert.gigi.util;

import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;

public class AuthorizationContext {

    CertificateOwner target;

    User actor;

    public AuthorizationContext(CertificateOwner target, User actor) {
        this.target = target;
        this.actor = actor;
    }

    public CertificateOwner getTarget() {
        return target;
    }

    public User getActor() {
        return actor;
    }

    public boolean hasRight(Group g) {
        return actor.isInGroup(g);
    }
}
