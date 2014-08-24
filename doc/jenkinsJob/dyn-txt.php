<?php
header("Content-type: text/plain");

if($_GET['token'] != "<here a secure password>"){
	die ();
}
$t1 = $_GET['t1'];
$t2 = $_GET['t2'];
if(!preg_match("/[a-zA-Z0-9]+/", $t1) || !preg_match("/[a-zA-Z0-9]+/", $t2)){
  die("Error");
}

$call = <<<EOF
server localhost
update delete cacert-{$t1}.<your fakezone here> TXT
update add cacert-{$t1}.<your fakezone here>  60 TXT {$t2}
send
quit

EOF;
echo $call;

$nsupdate = popen("/usr/bin/nsupdate -k <here your dnssec key>.key", 'w');
fwrite($nsupdate, $call);
$retval = pclose($nsupdate); // nsupdate doesn't return anything useful when called this way

?>
