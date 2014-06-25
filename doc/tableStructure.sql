DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL DEFAULT '',
  `password` varchar(255) NOT NULL DEFAULT '',
  `fname` varchar(255) NOT NULL DEFAULT '',
  `mname` varchar(255) NOT NULL DEFAULT '',
  `lname` varchar(255) NOT NULL DEFAULT '',
  `suffix` varchar(50) NOT NULL DEFAULT '',
  `dob` date NOT NULL DEFAULT '0000-00-00',
  `verified` int(1) NOT NULL DEFAULT '0',
  `ccid` int(3) NOT NULL DEFAULT '0',
  `regid` int(5) NOT NULL DEFAULT '0',
  `locid` int(7) NOT NULL DEFAULT '0',
  `listme` int(1) NOT NULL DEFAULT '0',
  `admin` tinyint(1) NOT NULL DEFAULT '0',
  `language` varchar(5) NOT NULL DEFAULT '',
  `created` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `modified` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `locked` tinyint(1) NOT NULL DEFAULT '0',
  `assurer_blocked` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `ccid` (`ccid`),
  KEY `regid` (`regid`),
  KEY `locid` (`locid`),
  KEY `email` (`email`),
  KEY `stats_users_created` (`created`),
  KEY `stats_users_verified` (`verified`),
  KEY `userverified` (`verified`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `email`;
CREATE TABLE `email` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `memid` int(11) NOT NULL DEFAULT '0',
  `email` varchar(255) NOT NULL DEFAULT '',
  `created` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `modified` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `hash` varchar(50) NOT NULL DEFAULT '',
  `attempts` int(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `memid` (`memid`),
  KEY `stats_email_hash` (`hash`),
  KEY `stats_email_deleted` (`deleted`),
  KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=latin1;

DROP TABLE IF EXISTS `pinglog`;
CREATE TABLE `pinglog` (
  `when` datetime NOT NULL,
  `uid` int(11) NOT NULL,
  `email` varchar(255) NOT NULL,
  `result` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

DROP TABLE IF EXISTS `baddomains`;
CREATE TABLE `baddomains` (
  `domain` varchar(255) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


DROP TABLE IF EXISTS `alerts`;
CREATE TABLE `alerts` (
  `memid` int(11) NOT NULL DEFAULT '0',
  `general` tinyint(1) NOT NULL DEFAULT '0',
  `country` tinyint(1) NOT NULL DEFAULT '0',
  `regional` tinyint(1) NOT NULL DEFAULT '0',
  `radius` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`memid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

DROP TABLE IF EXISTS `user_agreements`;
CREATE TABLE `user_agreements` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `memid` int(11) NOT NULL,
  `secmemid` int(11) DEFAULT NULL,
  `document` varchar(50) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `active` int(1) NOT NULL,
  `method` varchar(100) NOT NULL,
  `comment` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=61411 DEFAULT CHARSET=latin1;
