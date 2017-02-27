<?php
header("Content-type: text/plain");

define("ZONENAME", "your-zonename");
define("KEYNAME", "your/dns/update.key");
$appIdentifier = "someca";

if(!isset($_GET['token']) || !isset($_GET['t1']) || !isset($_GET['t2']) || !isset($_GET['action'])){
  die("Error");
}
$link = mysqli_connect("localhost", "db-user", "db-pw", "db");
if($_GET['token'] != "your-token-here"){
  die ();
}
$t1 = $_GET['t1'];
$t2 = $_GET['t2'];
if(!preg_match("/^[a-zA-Z0-9]+$/", $t1) || !preg_match("/^[a-zA-Z0-9]+$/", $t2)){
  die("Error");
}

$dnscalls = "";
if($t1!="purge"){
  $stmt = $link->prepare("INSERT INTO tokens SET type=?, name=?");
  $stmt->bind_param("ss", $type, $name);
  $type=$_GET['action'];
  $name = $t1;
  if($_GET['action'] == "http"){
    $stmt->execute();

    file_put_contents(".well-known/$appIdentifier-challenge/$t1.txt", $t2);
  } else if($_GET['action'] == "dns") {
    $stmt->execute();

    $dnscalls .= "update delete {$t1}._$appIdentifier._auth." . ZONENAME . " TXT\n"
      ."update add {$t1}._$appIdentifier._auth." . ZONENAME . " 60 TXT {$t2}\n";
  }
}
$stmt = $link->prepare("SELECT type, name FROM tokens WHERE created + 60000 < CURRENT_TIMESTAMP;");
$stmt->execute();

/* bind result variables */
$stmt->bind_result($type, $name);
$todelete = array();

/* fetch value */
while($stmt->fetch()){
  if($type == "http"){
    unlink(".well-known/$appIdentifier-challenge/{$name}.txt");
  } else if($type == "dns") {
    $dnscalls .= "update delete {$name}._$appIdentifier._auth." . ZONENAME . " TXT\n";
  }
  $todelete[] = array("type"=>$type, "name"=>$name);
}

$stmtd = $link->prepare("DELETE FROM tokens WHERE type=? AND name=?");
$stmtd->bind_param("ss", $type, $name);

foreach($todelete as $val){
  $type = $val["type"];
  $name = $val["name"];
  $stmtd->execute();
}

if($dnscalls != ""){
  dnsAction($dnscalls);
}

function dnsAction($command) {
  $call = "server localhost\n$command\nsend\nquit\n";

  $nsupdate = popen("/usr/bin/nsupdate -k " . KEYNAME, 'w');
  fwrite($nsupdate, $call);
  $retval = pclose($nsupdate); // nsupdate doesn't return anything useful when called this way
}
