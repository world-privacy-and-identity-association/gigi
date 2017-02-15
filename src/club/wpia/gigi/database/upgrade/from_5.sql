DROP TABLE IF EXISTS `passwordResetTickets`;
CREATE TABLE `passwordResetTickets` (
  `id` serial NOT NULL,
  `memid` int NOT NULL,
  `creator` int NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `used` timestamp NULL DEFAULT NULL,
  `token` varchar(32) NOT NULL,
  `private_token` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
);
