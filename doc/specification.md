= THIS DOCUMENT IS STILL WORK IN PROGRESS =

= Todo list =
== "Todo" (work in active code) ==

* Benchmarking (long time + load)

* review all date/time uses in order to watch over the timezone.

== Missing ==

* add email/domain ping
  * check for domains allowed in policy
  * black list for special domains not be use by user but through org
admins (e.g. adobe.com, 3m.com)
    * Maybe use Alexa Top 1M as one source
    * Use copies of DNS zones to identify new domains
    * Use DNSRBLs to identify young domains
  * List of well-known top level domains and Second-level sub domains
(cf. Public Suffix List below in references)
* OpenPGP integration
* think about org - certificates
* think about names. What are names? how many names? verify names?

== Makeup ==

** Extract menu from template
* Separate content and design


= Software requirements / wishes =
== Generic ==
* Useable without Javascript enabled
* Features implemented in Javascript may only enhance features already
accessible without Javascript
* All pages presented to the user must be HTML5-compliant
* All stylesheets must be valid CSS3
* All used scripts should be JavaScript/ECMAScript 1.6 or compatible
* For security reasons enforced by HSTS/CSP, the following restrictions
for included content apply
  * No inline Javascript
  * No inline CSS
  * Stylesheets, images, scripts are only allowed from a special
"static" subdomain

== Notifications ==
* Mail notifications from the system should use OpenPGP or S/MIME signatures

== Registration ==
 * set names (tbd)
 * set DoB (default: not set) (has to be before current date - not older
than 150 years, check for 29.2.?) (if underage: special ack?) INO: Why
not 120 year, which should be sufficent? (People are getting older, not
younger ... You know?)
 * set email(s) - at least one has to be set, primary needs to be set (tbd)
 * selection of allowed login methods (default: yes for PW and cert)
  * if PW login is allowed: set PW (tbd)  (always initially required as
it is hard to issue certificates directly on signup?) ES: If you could
do the email-check in the same session that is not mandatory. Even IF it
is, this could be done with a 1-time-PW from the mail. - Maybe the
complete step could be moved to AFTER the first email-check. It would
make more sense, and one could add more explanation and a link to
generate said certificate or whatever
  * selection if pw-recovery per questions should be possible
(explanatory text!)
   * if yes: pw-recovery q&a have to be set
 * confirmation that data was entered truthfully and recap of RLO at
this stage
 * Policy acceptance

== Login/authentication ==
* Separate Session scope for "www", "secure" and "api"
* Authenticate with email and password ("www" only)
* Authenticate with client certificate ("secure" + "api")
* Authenticate (temporary) with one-time access token (special
operations like revocation) (new feature)
* Option to switch off/on authentication methods (new feature)
* Option to selectively grant permissions to certain types of
credentials only (e.g. pw login may only verify someone, while cert XY
may do anything, SE interface with cert only) (new feature) ES: I think
Benny was speaking about a user setting, while INO was speaking about a
fixed global setting - this are two different things!
* Reset password by security questions and mail (?? probably not
[recommended in all situations])
* Option to switch off/on pw recovery per security questions (new feature)
* Reset password by verification
* Policy acceptance at login, if current version of Policies was not accepted by
user before

=== Account Flags (tbd: collect further (new) flags) ===
most should contain dates when set/removed - needed for account log and
audit
* Activated (Initial Account Activation Mail received)
* Agent Test passed (FD: virtual = live calculation)
* Agent (FD: virtual = live calculation)
* blocked Agent
* Policies accepted (virtual)
* blocked account
* deleted account
* PW login allowed (Global for Account)
* Cert login allowed (Global for Account, per Cert flags may set actual
permissions)
* PW Recovery function disabled
* PW Reset Required
* code signing allowed (Maybe better suited as a Permission for audit of
Dates)
* official nick name allowed (maybe needed for another new feature)
* nick name allowed (maybe needed for another new feature)
* announcements (general, country, regional, 200km) set
* involved in arbitration case (probably virtual, as additional
information has to be stored for those entries)

=== Special Account Permissions and Groups ===
TBL: Group [ID{AI/PK}, Name{S}]
TBL: GroupMembership [ID{AI/PK}, User{Ref(User)}, Group{Ref(Group)}]
TBL: Permission [ID{AI/PK}, Name{S}, ReportTo{S}]
TBL: UserPermissions [ID{AI/PK}, User{Ref(User)},
Permission{Ref(Permission)}, Action{E(Grant,Revoke)}, Date{Date}]
TBL: GroupPermissions [ID{AI/PK}, Group{Ref(Group)},
Permission{Ref(Permission)}, Action{E(Grant,Revoke)}, Date{Date}]

* Used for special permissions e.g. in support interface, mass mailings
* Additionally groups members with additional obligations (e.g. PP-Test)

* manual setting of permissions has to appear in personal and admin-log

== personal details (names, DoB) ==
* Change personal details (names, DoB) (only if not verified)
 * after each change, the user has to state, to have those names entered
in official documents
* maybe adding/removing of non-official nick-name allowed at any time
(new feature)
* Allow multiple names that can be verified independently (new) -
something like this should be clarified with the VO at least, better to
get it fixed clearly in an updated VPol (in general VPol does allow for this
at the moment, but people do not know about this)
* Allow to enter
 * 1 name - with a confirmation that one only has exactly one name part
 * Title? FN+ LN+ Suffix? marked as non-verifiable Nick? (could be used
for internal processes) - with a confirmation that those are covered by
at least one official document owned by the member/potential member
* GUI should (until further notice) allow only exactly ONE name (with
multiple name-parts) to be entered for an account
* Names containing a Nickname component MUST NOT be primary names for an
account (BB: Who did the striking? and Why?)
* official nicknames may be used, if switched on based on an verification
by support
* else nicknames may only be displayed as an addition and MAY NOT be verified
TBL: Name: [ID{AI/PK}, UID{Ref(User}},
TBL: NameComponent [ID{AI/PK}, Name{Ref(Name)}, Order{I},
Type{E[One,Firstname,Lastname,Title,Suffix,Nick,OfficialNick]},
Verifiable{B}, Value{S}, Context{S}, Points{I,Cached}]


=== Domain/email pinging ===
* re-ping domains regularly
  * Domains by automated variants like (see MoPad devDomainPing)
    * text files at certain locations
    * DNS TXT records
    * opening ssl connections

* configure types of pinging
* show previous pings with results. -> for support all, for member only
during their ownership (new feature)
* show status of the various pings and next scheduled events (new feature)
* Domain Pings record account ownership at time of ping (new feature)
* domain dispute (transfer of domains)
  * New owner files "Domain Dispute"
  * Old Owner receives mail with options to Accept, Decline or report
(to support) this claim
  * If Accepted domain is transferred (and existing certificates
referencing this domain revoked) [New feature: Old owner may specify
which ones, confirmed by new owner]
  * If any certificates should be kept, new owner is asked to confirm
(within certain amount of time) INO: There should be NO handover of cert
when moving domains (Needs discussion, new feature, maybe Policy Change
required)
  * Only after all certificates have either been revoked or been
confirmed by the new owner, the domain is finally transferred over.
  * If the transfer is Declined, no transfer is performed.
  * If the transfer is reported, information on both parties is sent to
support, including a description of the case (entered by the old owner
when reporting)
* email ping
  * For emails on a domain within the account
    * Reachability of the mail accounts is tried within random intervals
(silent)
    * If an email by our system is sent and properly accepted by the
receiving system, next try for explicit ping is postponed. (maximum
postponing of explicit check for about 1 year using this method) ES: has
big issues with a) grace period as it removes a big part of security
that we want to provide and b) automated contend-less mails as we
declare to not address members without reason - a ping would not be
considered a reason by our members BB: Outside the grace period the
email is considered unconfirmed, and no explicit ping is triggered, but
an active operation using this email address will cause a ping mail to
be sent. The main reason for this is to reduce the number of ping mails
sent when multiple certificates are issued in a given time span (UNDER
HEAVY DISCUSSION)
    * If any email address is failing for a certain time, schedule it to
be explicitly checked by a ping mail on next use in a certificate
    * a member should be able to initiate a ping mail
    * support should be able to initiate a ping (admin-log!)
    * If this explicit ping mail (with its included token/link) is not
confirmed in a reasonable time, a revalidation of the accompanying
domain is triggered, including a note to explicitly revalidate the
failing address)
    * permanent failing pings should be reported to primary email
address once
    * permanent failing pings should be displayed after a login (as
recent notifications)
  * For separate mail accounts (outside of domains that are part of an
account):
    * If regular mails from the system are properly received, postpone
automatic checking by at most one year (see ES's and BB's comments above)
  * send information/ping email before each certificate creation to
affected email(s),
    * this mail may be repeated if unsuccessful, but
    * HAS TO be successful eventually to complete the issuing of the
certificate for the affected email
    * selection per general setting if such mails must be confirmed
before the completion of the certificate issuing
    * failures have to be displayed directly and in logs
    * permanent failures have to be reported to primary email address once
    * permanent failures should be displayed after a login (as recent
notifications)
  * option to inform primary email, also (general setting + selectable
at certificate issue time)

== Certificate Management ==
* List all issued certificates per kind
* Revoke issued certificates
* renew certificates (re-issue a previous CSR) INO: what is if the old
CSR does not meet the requiremnts any more eg. weak keys -> Prefill of
the CreateCert-Dialog with the existing data for the cert/csr
* select md (sha2-512, sha2-384, sha2-256, sha3-512, whirlpool?, ...)
* select duration up to maximum allowed for member
* select (verified) name components / acronyms thereof
* select (ping-approved) domains / email-addresses
* select class (up to what is allowed for member)
* select if allowed for login as described above @ login
* change comment of certificate

MAYBE: * schedule to issue certificates for a day in the next two weeks.
(Should require someone who knows what he's doing, requires experienced
agent)

Notification Mail about new certificate may include link for revocation
(new feature)

Process for issuing a certificate:
1. Upload CSR / Generate key in Browser
   (That's actually step 2)1a.for browser generation: selection for
names, emails, domains, certificate root
2. Confirm values and if necessary modify them
3. Select additional settings (login, comments, ...) and validity interval
4. confirm policies + confirm the issuing of the certificate as shown
(do 1a to 4 in one form? - Could be done with JS enabled)
5. send information (ping) mail to affected address (& primary if
selected), display failures + potential abort
6. Show Progress Page with JS hinting when it is done / otherwise using
redirects in HTML
7. Display a page with the final Certificate and details about it.

Process of re-issuing certificate:
1. click certificate (this creates the form-process)
2. review all previous-entered settings. (inputs will be re-checked when
sent: is name still valid/etc. still have all rights.)
3. rest as above, formulations adapted to re-issuing

=== Certificate types ===
* Email certificates (include email (+ real name if points>=50)
(+codesigning if enabled && Agent)
* Domain certificates (include domain name + SANs (+real name if
points>=50))
* Organisation Email certificates
* Organisation Domain certificates
* Split more templates (e.g. Email from Code Signing)

=== Verification management ===
* Verify a member
  * enter email address + DoB
    * if correct:
      * if member was verified before by this agent:
        * most recent verification overrides previous ones
      * if there was no previous verification by the agent over the member:
        * names shown [has to be defined in more detail]
        * DoB shown
        * primary email shown
        * confirmation that names + DoB could be verified were official
documents
        * select if TTP [further details TBD]
        * free-text-field: location of verification meeting - default: empty - needs to be filled
        * date of verification selection, default: non selected (not before DoB, not after current date+13h) - needs to be set
          INO: Current Date UTC + 12 h
          BB: (13h, cause of timezones with +13h)
        * confirmation that member has accepted the policies
        * confirmation box, that VPol was followed
        * Policy acceptance box for agent
    * if not correct independent of which was wrong:
      * option to write an automated email to the member that the agent tried to verify the member
* View all verifications received/done
  * points, locations, dates of verification, dates of verification entered, name of agent/member, revocations
* show how my points are calculated
  * as member, as agent
* search for agents/etc (what exactly is this community-thing)
  * Search for agent is referred to seperate site
* What is about multiple verifications for the same member -> only last verification should count. (New Feature)
* Experience Points only for the last of such re-verifications (New Features)

=== Organisation management ===

* rethink!
  * what needs to be done?
  * what needs to be possible?
  * how much is this used?
* The Org area can be shifted back for now.

Roles:
* OrgAgent: can create and administer the OrgAccounts, administer: add and delete OrgAdmin, add and delete Org Domains, does not have access to the certs
* OrgMaster: can see the Org account data; add/remove other OrgMasters and OrgAdmins
* OrgAdmin: can see the org account data; is able to administer the certs
* SE: is able to see account data, revoke certs (new feature)

Cert Creation:
* Client Cert with form
* Client Cert with CSR (New feature)
* Client Cert with multiple email addresses
* Multiple Client Certs via file upload and result download (create e.g. 10 certs with one upload) (New Feature)
* revoke cert via file upload, e.g. file with to email addresses all certs to these addresses will be revoked. (New Feature)

* Filter on client cert page (new feature)

* Domain Cert with CSR

* Org Account History
  * Org account full history visible by OrgAdmins, SE via Ticket Number
  * Org Account history only account information without cert history by OrgAgent

* OrgAgent should be able to perform an IsAssuer check.



=== OpenPGP key signing ===
* if points >= 50 allow signing of OpenPGP key with real name, no comment field, matching email in account.
* UserIDs on key to be signed user-selectable.
* Expiry date: selectable up to maximum allowed
* re-signing without need to uploade key again
* Policy acceptance needed for all signatures
* lookup of keys on keyserver (if present)
* Ensure key has basic cryptographic security:
  * conformant to normal certificate's requirements
  * valid key material

=== Signer interface ===
* database based
* allow multiple signer instances
* job table

=== SE management (not finished) ===
* nothing happens without support ticket id (that is verified?) <- ES:
currently the basic account can be seen without a ticket number
  * Support Ticket, Arbitration; see existing software on this <- ES:
currently other tickets are allowed, too, but I see no need, as one of
both should be needed (there will always be a support ticket number, if
support is addressed as such - there should be no need for anybody to
neither go this way nor through arbitration (arbitration may have
reasons to go directly to a supporter but has the authority to do so)
* every action on a member will cause an email to be sent to him and the
action is being logged. <- the mail should be optional (to primary
address) and only once in a given time frame/login, as a supporter may
need to do more than one thing at a time
* View/edit users personal details (even if verified)
* set account flags (code signing, Agent status, block of account, org-admin, support, team memberships)
* delete/invalidate verifications
* revoke certificates, pgp-key signatures, both should be possible for single certificates/keys or for all certificates/keys
* "delete" account

=== automated (outside of a current process) information mails to members ===
 * certificate(s)/signature expires within the next 14 days (combined or separate)

=== "mass mails" interface (new feature) ===
  * access only for critical team (or other restricted personal controlled by arbitration)
  * arbitration case number needed (maybe announcement flags could be allowed for support case numbers or motions, as well)
  * requester has to be set (identified by email)
  * text-field for subject
  * text-field for mail-content
  * one of the following three should be possible for selection of recipients:
    * sql-query
    * list of email addresses
    * list of account-ids
    * selection field if this should be noted in account-logs of recipients
    * announcement flag (general, ...)

=== Statisics ===

* running total for current year
  * verifications
  * new members
  * new agents
  * lost members
  * active agents (at least one verification according to the verification date of the current year)
  * active members (who got their latest verification in the current year)

* running total for last 12 months per month
  * verifications
  * new member
  * new agents
  * lost members
  * new certs
  * revoked certs
  * active agents / members

* Grand Total
  * members
  * verifications
  * granted VP
  * user >= 50 and <99 VP
  * agent candidates
  * agents
  * issued client certs
  * active client certs
  * revoked client certs
  * issued server certs
  * active server certs
  * revoked server certs
  * issued org client certs
  * active org client certs
  * revoked org client certs
  * issued org server certs
  * active org server certs
  * revoked org server certs
  * issued gpg certs
  * active gpg certs
  * revoked gpg certs

* yearly statistics
  * verifications
  * new member
  * new agents
  * lost members
  * new certs
  * revoked certs
  * active agents (at least one verification according to the verification date of the current year)
  * active members

access statistic data via API e.g. verifications per year is needed for coaudit database

the active members may be relevant for any decision in the regard how long verifications need be valid and if they need to be renewed.

=== Premission Review mails (monthly?) ===
* Regular check and report of permissions for various groups of people
  INO: currently quarterly

=== Tools ===
* csr/crt inspector
* + lookup OCSP/CRL status.
* OpenPGP key inspector (Yup, we need one)
* Database Editor CLI tool for crit to update records (and their checksums/timestamping)
