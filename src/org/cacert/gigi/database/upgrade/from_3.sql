DROP TABLE IF EXISTS `profiles`;
CREATE TABLE `profiles` (
  `id` int(3) NOT NULL AUTO_INCREMENT,
  `keyname` varchar(60) NOT NULL,
  `include` varchar(200) NOT NULL,
  `requires` varchar(200) NOT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE (`keyname`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;
