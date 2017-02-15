DROP TABLE IF EXISTS `cacerts`;
CREATE TABLE `cacerts` (
  `id` int(3) NOT NULL AUTO_INCREMENT,
  `keyname` varchar(60) NOT NULL,
  `link` varchar(160) NOT NULL,
  `parentRoot` int(3) NOT NULL,
  `validFrom` datetime NULL DEFAULT NULL,
  `validTo` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE (`keyname`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;
