== Glossary / Definitions ==

ASN.1: A horrible way to encode data. Usually used together with X.509

BER: Basic Encoding Rules for ASN.1

CER: Canonical Encoding Rules for ASN.1

CSR: Certificate Signing Request, request to get some public key signed

CSRF: Cross Site Request Forgery, attach technique breaching causality
of requests

DER: Distinguished Encoding Rules for ASN.1

ECMA: European Computer Manufacturers Association

ETSI: European Telecommunications Standards Institute

GnuPG: GNU Privacy Guard, Some implementation using the OpenPGP standard

HSTS: Hypertext Strict Transport Security, Protection Mechanism against
casual MitM in networks and SSL Stripping, governed by RFC 6797

ITU: International Telecommunication Union, standards body responsible
for most standards with a dot in their names

JS: JavaScript, an ECMA-Standard

JSON: JavaScript Object Notation, standardized way to encode data for
easy parsing

MIME: Multipurpose Internet Mail Extensions, some way to stuff multiple
messages into one message

OAuth: OpenAuthentication standard for SSO

OpenPGP: Signature and Encryption format governed by RFC 4880 et. al.

OTP: One-Time-Password

PKI: Public Key Infrastructure

PKIX: PKI using X.509

SPKAC: Signed Public Key and Challenge, interactive variant of a CSR

SSL: Secure Socket Layer, predecessor of TLS, cf. TLS

SSO: Single Sign On, mechanism for authentication accross different
domains/systems using a central identity

TLS: Transport Layer Security, Protocol for secure communication between
a client and a server, governed by various RFCs

X.509: An ITU standard describing contents of things (usually abused for
PKIX certificates)

XSS: Cross-Site Scripting, attack technique breaching same-origin boundaries

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
* think about names. What are names? how many names? assure names?

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
 * CCA acceptance

== Login/authentication ==
* Separate Session scope for "www", "secure" and "api"
* Authenticate with email and password ("www" only)
* Authenticate with client certificate ("secure" + "api")
* Authenticate (temporary) with one-time access token (special
operations like revocation) (new feature)
* Option to switch off/on authentication methods (new feature)
* Option to selectively grant permissions to certain types of
credentials only (e.g. pw login may only assure someone, while cert XY
may do anything, SE interface with cert only) (new feature) ES: I think
Benny was speaking about a user setting, while INO was speaking about a
fixed global setting - this are two different things!
* Reset password by security questions and mail (?? probably not
[recommended in all situations])
* Option to switch off/on pw recovery per security questions (new feature)
* Reset password by assurance (how is this going to be implemented
automatically?) Cannot be done automatically at the moment, as this is
covered by a process clearly defined by an arbitration precedents-case.
- At least not with a new precedents ruling. - Only part is the pw-reset
field on the SE side
* CCA acceptance at login, if current version of CCA was not accepted by
user before

=== Account Flags (tbd: collect further (new) flags) ===
most should contain dates when set/removed - needed for account log and
audit
* Activated (Initial Account Activation Mail received)
* CATs passed (FD: virtual = live calculation)
* assurer (FD: virtual = live calculation)
* blocked assurer
* CCA accepted (virtual)
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
* Additionally groups members with additional obligations (e.g. PP-CATS)

* manual setting of permissions has to appear in personal and admin-log

== personal details (names, DoB) ==
* Change personal details (names, DoB) (only if not assured)
 * after each change, the user has to state, to have those names entered
in official documents
* maybe adding/removing of non-official nick-name allowed at any time
(new feature)
* Allow multiple names that can be assured independently (new) -
something like this should be clarified with the AO at least, better to
get it fixed clearly in an updated AP (in general AP does allow for this
at the moment, but people do not know about this)
* Allow to enter
 * 1 name - with a confirmation that one only has exactly one name part
 * Title? FN+ LN+ Suffix? marked as non-assurable Nick? (could be used
for internal processes) - with a confirmation that those are covered by
at least one official document owned by the member/potential member
* GUI should (until further notice) allow only exactly ONE name (with
multiple name-parts) to be entered for an account
* Names containing a Nickname component MUST NOT be primary names for an
account (BB: Who did the striking? and Why?)
* official nicknames may be used, if switched on based on an assurance
by support
* else nicknames may only be displayed as an addition and MAY NOT be assured
TBL: Name: [ID{AI/PK}, UID{Ref(User}},
TBL: NameComponent [ID{AI/PK}, Name{Ref(Name)}, Order{I},
Type{E[One,Firstname,Lastname,Title,Suffix,Nick,OfficialNick]},
Assureable{B}, Value{S}, Context{S}, Points{I,Cached}]


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
* select (assured) name components / acronyms thereof
* select (ping-approved) domains / email-addresses
* select class (up to what is allowed for member)
* select if allowed for login as described above @ login
* change comment of certificate

MAYBE: * schedule to issue certificates for a day in the next two weeks.
(Should require someone who knows what he's doing, requires experienced
assurer)

Notification Mail about new certificate may include link for revocation
(new feature)

Process for issuing a certificate:
1. Upload CSR / Generate key in Browser
   (That's actually step 2)1a.for browser generation: selection for
names, emails, domains, certificate root
2. Confirm values and if necessary modify them
3. Select additional settings (login, comments, ...) and validity interval
4. confirm CCA + confirm the issuing of the certificate as shown
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
(+codesigning if enabled && Assurer)
* Domain certificates (include domain name + SANs (+real name if
points>=50))
* Organisation Email certificates
* Organisation Domain certificates
* Split more templates (e.g. Email from Code Signing)

=== WoT management ===
* Assure a member
  * enter email address + DoB
    * if correct:
      * if member was assured before by this assurer:
        * currently: abort process with according information - but:
this may be changed later
      * if there was no previous assurance by the assurer over the member:
        * names shown [has to be defined in more detail]
        * DoB shown
        * primary email shown
        * confirmation that names + DoB could be verified were official
documents
        * select if TTP [further details TBD]
        * free-text-field: location of assurance - default: empty -
needs to be filled
        * date of assurance selection, default: non selected (not before
DoB, not after current date+13h) - needs to be set INO: Current Date UTC
+ 12 h BB:(13h, cause of timezones with +13h)
        * confirmation that assuree has accepted CCA
        * confirmation box, that AP was followed
        * CCA acceptance box for assurer
        * display if member was underage at assurance date, when set
[TBD] and force a check-box-selection to confirm that PoJam process was
followed
    * if not correct independent of which was wrong:
      * option to write an automated email to the member that the
assurer tried to assure the member
* View all assurances received/done
  * points, locations, dates of assurance, dates of assurance entered,
name of assurer/assuree, revocations
* show how my points are calculated
  * as assuree, as assurer
* search for assurers/etc (what exactly is this community-thing)
  * Search for assurer is referred to Portal (cacert.eu) ES: I really
would like to have this feature again in the main software- in both
directions, with the option to enter multiple locations at least
temporary (permanent location, current location), we should make it easy
for travelling assurer and assurees to spread assurances around! BB: I
agree to ES: Having it in the software makes many things much more easy.
* What is about multiple assurances for the same assuree, only last
Assurance should count. (New Feature) - ES: currently this would need
more details in policies - we do not know how this would resolve, we may
only guess that it would be the last one
* Experience Points only for the last of such re-assurances (New
Features) - ES: again, this should wait for a policy decision

=== Organisation management ===

rethink? what needs to be done? what needs to be possible? how much is
this used?
* The Org area can be shifted back for now.


Roles:
* OrgAssurer: can create and administer the OrgAccounts, administer: add
and delete OrgAdmin, add and delete Org Domains, does not have access to
the certs
* OrgMaster: can see the Org account data; add/remove other OrgMasters
and OrgAdmins
* OrgAdmin: can see the org account data; is able to administer the certs
* SE: is able to see account data, revoke certs (new feature)

Cert Creation:
* Client Cert with form
* Client Cert with CSR (New feature)
* Client Cert with multiple email addresses
* Multiple Client Certs via file upload and result download (create e.g.
10 certs with one upload) (New Feature)
* revoke cert via file upload, e.g. file with to email addresses all
certs to these addresses will be revoked. (New Feature)

* Filter on client cert page (new feature)

* Domain Cert with CSR

* Org Account History
  * Org account full history visible by OrgAdmins, SE via Ticket Number
  * Org Account history only account information without cert history by
OrgAssurer

* OrgAssurer should be able to perform an IsAssuer check.



=== OpenPGP key signing ===
* if points >= 50 allow signing of OpenPGP key with real name, no
comment field, matching email in account.
* UserIDs on key to be signed user-selectable.
* Expiry date: selectable up to maximum allowed
* re-signing without need to uploade key again
* CCA acceptance needed for all signatures
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
* View/edit users personal details (even if assured)
* set account flags (code signing, assurer status, block of account,
ttp, org-admin, support, team member)
* delete/invalidate assurances
* revoke certificates, pgp-key signatures, both should be possible for
single certificates/keys or for all certificates/keys
* "delete" account

=== Arbitration interface (new feature) ===
 * everything requires: arbitration number, role in arbitration case
(iCM, CM, Arbitrator)
 * everything has to be shown in admin log for support and in account log
 * show CCA acceptance for user identified by email (does not have to be
primary) (only latest version of accepted CCA)
 * show primary email for email address
 * show arbitration case number of arbitration cases, a user identified
per email address is involved in
 * mark/unmark user identified per email (does not have to be primary)
as involved in an arbitration case (by numbers)
 * set CCA acceptance for user identified by email (date, "before
arbitrator")
 * running arbitration case should be visible to SE - ES: That is NOT
part of the arbitration interface! And currently it would be enough, if
delete account process would be stopped by the member being involved in
an arb case, telling that there is an arb case (not which and how many)
- currently this information is not needed in any other support case and
involvement in arbitration cases is anonymous.

=== automated (outside of a current process) information mails to
members ===
 * certificate(s)/signature expires within the next 14 days (combined or
separate)

=== "mass mails" interface (new feature) ===
  * access only for critical team (or other restricted personal
controlled by arbitration)
  * arbitration case number needed (maybe announcement flags could be
allowed for support case numbers or motions, as well)
  * requester has to be set (identified by email)
  * text-field for subject
  * text-field for mail-content
  * one of the following three should be possible for selection of
recipients:
    * sql-query
    * list of email addresses
    * list of account-ids
    * selection field if this should be noted in account-logs of recipients
    * announcement flag (general, ...)

=== Statisics ===

* running total for current year
  * assurances
  * new member
  * new assurers
  * lost members
  * active assurers (at least one assurance according to the assurance
date of the current year)
  * active assurees (who got the latest assurance in the current year)

* running total for last 12 months per month
  * assurances
  * new member
  * new assurers
  * lost members
  * new certs
  * revoked certs
  * active assurer / assurees

* Grand Total
  * members
  * assurances
  * granted AP
  * user >= 50 and <99 AP
  * assurer candidates
  * assurer
  * issued client certs
  * active client certs
  * issued server certs
  * active server certs
  * issued org client certs
  * active org client certs
  * issued org server certs
  * active org server certs
  * issued gpg certs
  * active gpg certs

* yearly statistics
  * assurances
  * new member
  * new assuer
  * lost members
  * new certs
  * revoked certs
  * active assurers (at least one assurance according to the assurance
date of the current year)
  * active assurees

access statistic data via API e.g. assurance per year is needed for
coaudit database

the active assurees may be relevant for any decision in the regard how
long assurances should be valid and if they need to be renewed. It also
gives

=== Premission Review mails (monthly?) ===
* Regular check and report of permissions for various groups of people
INO: currently quarterly

=== Tools ===
* csr/crt inspector
* + lookup OCSP/CRL status.
* OpenPGP key inspector (Yup, we need one)
* Database Editor CLI tool for crit to update records (and their
checksums/timestamping)

==References==
* Our Policies
  * CAcert Community Agreement (CCA)
  * Dispute Resolution Policy (DRP)
  * Certificate Policy on Signing (CPS) + Certification Policy (per
Root) (CP)
  * Security Policy (SP) / Security Manual (SM)
  * Assurance Handbook (AH) + Practice on Names (PoN)
    * Policy on Junior Assurers and Members (PoJAM)
    * Trusted Third Party (TTP) and Sub-Policies
    * Organisation Assurance Policy (OAP)
  * Policy on Policy (PoP)
  * CAcert Website Data Privacy Policy (currently WIP) WDPP
* Internet Standards
  * RFC documents
    * IPv4/IPv6
      * RFC 0791 (IPv4: Internet Protocol, 1981)
      * RFC 2460 (IPv6: Internet Protocol, Version 6 (IPv6)
Specification, 1998)
    * HTTP
      * RFC 1945 (HTTP/1.0, 1996)
      * RFC 2616 (HTTP/1.1, 1999)
      * RFC 7230 (HTTP/1.1: Message Syntax and Routing, 2014)
      * RFC 7231 (HTTP/1.1: Semantics and Content, 2014)
      * RFC 7232 (HTTP/1.1: Conditional Requests, 2014)
      * RFC 7233 (HTTP/1.1: Range Requests, 2014)
      * RFC 7234 (HTTP/1.1: Caching, 2014)
      * RFC 7235 (HTTP/1.1: Authentication, 2014)
    * SSL/TLS
      * RFC 3268 (TLS 1.0+: Advanced Encryption Standard (AES)
Ciphersuites for Transport Layer Security (TLS), 2002)
      * RFC 4347 (DTLS 1.0: Datagram Transport Layer Security, 2006)
      * RFC 4492 (TLS 1.0+: Elliptic Curve Cryptography (ECC) Cipher
Suites for Transport Layer Security (TLS), 2006)
      * RFC 4346 (TLS 1.1: The Transport Layer Security (TLS) Protocol
Version 1.1, 2006)
      * RFC 4366 (TLS 1.0+: Transport Layer Security (TLS) Extensions, 2006)
      * RFC 5246 (TLS 1.2: The Transport Layer Security (TLS) Protocol
Version 1.2, 2008)
      * RFC 5764 (TLS 1.0+: Transport Layer Security (TLS) Renegotiation
Indication Extension, 2010)
      * RFC 5878 (TLS 1.0+: Transport Layer Security (TLS) Authorization
Extensions, 2010)
      * RFC 6176 (SSL 2.0: Prohibiting Secure Sockets Layer (SSL)
Version 2.0, 2011)
      * RFC 7027 (TLS 1.0+: Elliptic Curve Cryptography (ECC) Brainpool
Curves for Transport Layer Security (TLS), 2013)
    * Web Security
      * RFC 6797 (HSTS: HTTP Strict Transport Security, 2012)
    * PKIX
      * RFC 2459 (X.509: Internet X.509 Public Key Infrastructure
Certificate and CRL Profile, 1999)
      * RFC 3280 (X.509: Internet X.509 Public Key Infrastructure
Certificate and Certificate Revocation List (CRL) Profile, 2002)
      * RFC 4325 (X.509: Internet X.509 Public Key Infrastructure
Authority Information, Access Certificate Revocation List (CRL)
Extension, 2005)
      * RFC 4630 (X.509: Update to DirectoryString Processing in the
Internet X.509 Public Key Infrastructure Certificate and Certificate
Revocation List (CRL) Profile, 2006)
      * RFC 5280 (X.509: Internet X.509 Public Key Infrastructure
Certificate and Certificate Revocation List (CRL) Profile, 2008)
    * OpenPGP
      * RFC 1991 (OpenPGP: PGP Message Exchange Formats, 1996)
      * RFC 2440 (OpenPGP: OpenPGP Message Format, 1998)
      * RFC 4880 (OpenPGP: OpenPGP Message Format, 2007)
      * RFC 5581 (OpenPGP: The Camellia Cipher in OpenPGP, 2009)
      * RFC 6637 (OpenPGP: Elliptic Curve Cryptography (ECC) in OpenPGP,
2012)
    * JSON
      * RFC 4627 (JSON: The application/json Media Type for JavaScript
Object Notation (JSON), 2006)
      * RFC 7158 (JSON: The JavaScript Object Notation (JSON) Data
Interchange Format, 2013)
      * RFC 7159 (JSON: The JavaScript Object Notation (JSON) Data
Interchange Format, 2013)
      * ECMA 404 (JSON: The JSON Data Interchange Format, 2013)

http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf
    * MIME
      * RFC 2045 (MIME: Multipurpose Internet Mail Extensions (MIME)
Part One: Format of Internet Message Bodies, 1996)
      * RFC 2046 (MIME: Multipurpose Internet Mail Extensions (MIME)
Part Two: Media Types, 1996)
      * RFC 2047 (MIME: MIME (Multipurpose Internet Mail Extensions)
Part Three: Message Header Extensions for Non-ASCII Text, 1996)
      * RFC 2048 (MIME: Multipurpose Internet Mail Extensions (MIME)
Part Four: Registration Procedures, 1996)
      * RFC 2049 (MIME: Multipurpose Internet Mail Extensions (MIME)
Part Five: Conformance Criteria and Examples, 1996)
      * RFC 2183 (MIME: Communicating Presentation Information in
Internet Messages: The Content-Disposition Header Field, 1997)
      * RFC 2184 (MIME: MIME Parameter Value and Encoded Word
Extensions: Character Sets, Languages, and Continuations, 1997)
      * RFC 2231 (MIME: MIME Parameter Value and Encoded Word
Extensions: Character Sets, Languages, and Continuations, 1997)
      * RFC 5335 (MIME: Internationalized Email Headers, 2008)
      * RFC 6532 (MIME: Internationalized Email Headers, 2012)
    * S/MIME
      * RFC 1847 (S/MIME: Security Multiparts for MIME: Multipart/Signed
and Multipart/Encrypted, 1995)
      * RFC 2633 (S/MIME: S/MIME Version 3 Message Specification, 1999)
      * RFC 3851 (S/MIME: Secure/Multipurpose Internet Mail Extensions
(S/MIME) Version 3.1 Message Specification, 2004)
      * RFC 5751 (S/MIME: Secure/Multipurpose Internet Mail Extensions
(S/MIME) Version 3.2 Message Specification, 2010)
  * W3C documents
    * HTML 5
      * http://www.w3.org/TR/html-markup/
      * http://dev.w3.org/html5/html4-differences
    * CSS 3
      * http://www.w3.org/Style/CSS/
    * JavaScript / ECMAScript
      *
http://standards.iso.org/ittf/PubliclyAvailableStandards/c055755_ISO_IEC_16262_2011(E).zip
      * http://ecma-international.org/ecma-262/5.1/
    * XML
      * XML 1.0: http://www.w3.org/TR/2008/REC-xml-20081126/
      * XML 1.1: http://www.w3.org/TR/2006/REC-xml11-20060816/
    * Content Security Policy
      * Version 1.0: http://www.w3.org/TR/CSP/
      * Version 1.1 (WIP):
https://w3c.github.io/webappsec/specs/content-security-policy/
  * Miscellaneous
    * Public Suffix List of
      http://publicsuffix.org/
  * Unicode Standard
    * Unicode Technical Report 39: http://www.unicode.org/reports/tr39/
* ITU Standards
  * X.690 / ASN.1
    * Information technology – ASN.1 encoding rules: Specification of
Basic Encoding Rules (BER), Canonical Encoding Rules (CER) and
Distinguished Encoding Rules (DER)
      http://www.itu.int/ITU-T/studygroups/com17/languages/X.690-0207.pdf
  * X.509
* CA/Browser Forum
  * Baseline Requirements
    https://cabforum.org/baseline-requirements-documents/

* Miscellanious
  * Passwords
    * Research on Password strength
      Carnegie Mellon University: Guessing again (and again and again)
      https://www.ece.cmu.edu/~lbauer/papers/2012/oakland2012-guessing.pdf
      Presentation on the findings of the paper:
      https://www.youtube.com/watch?v=USMd3swFZp4
    * SCrypt Key Dervation Function
      http://www.tarsnap.com/scrypt.html
      Colin Percival, Stronger Key Derivation via Sequential Memory-Hard
Functions, presented at BSDCan'09, May 2009:
      * http://www.tarsnap.com/scrypt/scrypt.pdf
      * http://www.tarsnap.com/scrypt/scrypt-slides.pdf
