<?php
header("Content-type: text/plain");

define("ZONENAME", "cacert.dyn.dogcraft.de");
define("KEYNAME", "keys/Kcacert.dyn.dogcraft.de.+165+54687.key");

if(!isset($_GET['token']) || !isset($_GET['t1']) || !isset($_GET['t2']) || !isset($_GET['action'])){
  die("Error");
}
if($_GET['token'] != "rD1m3A9ew6Hs4DIv7lnTxNbR6dr"){
  die ();
}
$t1 = $_GET['t1'];
$t2 = $_GET['t2'];
if(!preg_match("/^[a-zA-Z0-9]+$/", $t1) || !preg_match("/^[a-zA-Z0-9]+$/", $t2)){
  die("Error");
}
$todelete = array();

if(file_exists("data.php")){
  include ("data.php");
}

$time = time()/60;
if(!isset($todelete[$time])){
  $todelete[$time] = array();
}

$dnscalls = "";

if($_GET['action'] == "http"){
  $todelete[$time][] = array("http", $t1);
  file_put_contents("cacert-$t1.txt", $t2);
} else if($_GET['action'] == "dns") {
  $todelete[$time][] = array("dns", $t1);
  $dnscalls .= "update delete {$t1}._cacert._auth." . ZONENAME . " TXT\n"
    ."update add {$t1}._cacert._auth." . ZONENAME . " 60 TXT {$t2}\n";
}
$copy = $todelete;
foreach($copy as $nt => $ar){
  if($nt < $time - 2){
    unset($todelete[$nt]);
    foreach($ar as $act){
      if($act[0] == "http"){
        unlink("cacert-{$act[1]}.txt");
      } else if($act[0] == "dns") {
        $dnscalls .= "update delete {$act[1]}._cacert._auth." . ZONENAME . " TXT\n";
      }
    }
  }
}
file_put_contents("data.php", "<?php \$todelete = ".var_export($todelete,true).";\n?>");

if($dnscalls != ""){
  dnsAction($dnscalls);
}

function dnsAction($command) {
  $call = "server localhost\n$command\nsend\nquit\n";

  $nsupdate = popen("/usr/bin/nsupdate -k " . KEYNAME, 'w');
  fwrite($nsupdate, $call);
  $retval = pclose($nsupdate); // nsupdate doesn't return anything useful when called this way
}

