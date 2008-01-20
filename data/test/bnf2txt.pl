#!/usr/local/bin/perl
# name: bnf2txt.prl
# author: Chris Wallace
# date: 21 May 1997 
# purpose: generate a sample string as defined by the BNF grammer input
# input :  command line arguments
#    random number seed
#    number of sentences to generate
#    separator
#   stdin - bnf file with terms as strings, terminals enclosed in ""
#          in this version, mixed constructs are not allowed 
#          i.e. a rule is either a sequence, a selection, or an iteration
#          definitions followed by optional distribution spec
#          #  distcode p1 p2
#    e.g.   U           - iteration : uniform in range min ... max
#           B  p        - iteration : binomial probability p
#           Z           - selection  : Zipfian distribution 
# process: 
#    the bnf definition is parsed,using a tokeniser and some hand-coded
#    the rules are held in three dictionaries, all indexed by the LHS
#         rule containing the type of rule seq, sel or itr
#         terms containing the sequences of terms
#         dist containing a specification of the distribution to use
#    starting with the first definition, rules are recursively expanded, and
#    random choices made from iterations or selections according to the 
#    specified or default distribution

# output: stdout - the generated strings
#
#   allow  \ escaped characters
#   terminal separator should be included in bnf file as directive
#   Zipf distribution - build during loading instead of on first call
#     after generation, looks no different to an empirical cdf
#   empirical cdf
#   random starting seed required
#   must be able to put a maximum as well as a minimum on a Binomial iteration
#   parameterised terms 
#
# new problems
#  need some error reporting
#
# Change history
#  updated 20 feb 
#    convert to cgi
#    rewrite to use tokeniser
#  24th Feb batch version    
#-----------------------------------------------------------
# &getFormParams;
 &getCommandParams;
 &loadbnf(@BNF);
 
# &generateHTML;
  &generateText;

#--------------------------------------------------------------------
sub generateText {
# &printbnf();

 for($i=0;$i<$NSENTENCES;$i++) {
   &expand($orderedTerms[0]);
    print "\n";
 }
}


#--------------------------------------------------------------------
sub generateHTML {
print <<END;
Content-type: text/html

<html><head><title>Random sentence generator</title></head>
<body>
<h2>Random BNF Generator</h2>
<table> <tr><td><a href=bnfhelp.html>Help</a><td><a href=examples.html>Examples</a>
</table>
<br>
END

#generate sentences

 for($i=0;$i<$NSENTENCES;$i++) {
   &expand($orderedTerms[0]);
    print "<br>\n";
 }
#generate the rest of the HTML
print <<END;
<form method=post action=bnf2sent.cgi>
<input type=submit value=Generate>
<br>BNF text 
<br><textarea name=bnf rows=10 cols=80>
END
foreach $line (@BNF) {
   &trim($line);
   print $line;
}
print <<END;
</textarea> 
<br>Number of sentences to generate: <input name=number value=$NSENTENCES size=4>
<br>Seed for random Numbers: <input name=seed value=$SEED size=6>
<br>Use space to separate tokens: <input type=checkbox name=space $checked>

</form>
</body>
</html>
END
}
#--------------------------------------------------------------------------
sub getCommandParams{

 $SEED = shift(@ARGV);
 if (!$SEED) {$SEED=123456;}
 srand($SEED);
 $SEED++;  
 $NSENTENCES = shift(@ARGV);
 if (!$NSENTENCES) {$NSENTENCES=5;}
 $SEP = shift(@ARGV);
 if (!$SEP) {$SEP='';}   
 $PROB = shift(@ARGV);
 if (!$PROB) {$PROB= 0.5;}
 $BLANK = '_';
 @lines = <>;
 @BNF = @lines;

}

#--------------------------------------------------------------------------
sub getFormParams {
 use CGI qw(:standard);
 $SEED = param('seed');
 if (!$SEED) {$SEED=123456;}
 srand($SEED);
 $SEED++;  
 $NSENTENCES = param('number');
 if (!$NSENTENCES) {$NSENTENCES=5;}
 if (param('space')) {
   $SEP = ' '; $checked ='checked';}   
 else {
   $SEP=''; $checked = '';} 
 $PROB = param('probability');
 if (!$PROB) {$PROB= 0.5;}
 $BLANK = '_';
 $bnfstr = param('bnf');
 $bnfstr.="\n";
 @BNF = split('\n',$bnfstr);
}

#----------------------------------------------------------------------------
sub tokenize {
 local($st) = shift(@_).' ';
 local(@tokens)=();
 local($i,$c,$t);
 $i=0;$s=0;
 $c=substr($st,$i,1);$i++;
 while ($i < length($st)) {
#  print "$s; $c; $i \n";
  if ($s==0 && $c=~/\s/){   $c=substr($st,$i,1);$i++; next;}
  if ($s==0 && $c=~/\d/){   $t=$c; $c=substr($st,$i,1);$i++; $s=2; next;}
  if ($s==2 && $c=~/[\d\.]/){   $t.=$c; $c=substr($st,$i,1);$i++; next;}
  if ($s==2 && $c=~/\D/){   $t="N\t$t";push(@tokens,$t); $s=0; next;}
  if ($s==0 && $c=~/\w/){   $t=$c; $c=substr($st,$i,1);$i++; $s=1; next; }
  if ($s==1 && $c=~/[\w_]/){   $t.=$c; $c=substr($st,$i,1);$i++; next;}
  if ($s==1 && $c=~/\W/){   $t="W\t$t";push(@tokens,$t); $s=0; next;}
  if ($s==0 && $c=~/'/) {   $t="'"; $c=substr($st,$i,1);$i++; $s=3; next;}
  if ($s==3 && $c=~/[^']/){ $t.=$c; $c=substr($st,$i,1);$i++; next;}
  if ($s==3 && $c=~/'/) {   $t="S\t$t'";push(@tokens,$t); $c=substr($st,$i,1);$i++; $s=0; next;}
  if ($s==0 && $c=~/"/) {   $t="'"; $c=substr($st,$i,1);$i++; $s=4; next;}
  if ($s==4 && $c=~/[^"]/){ $t.=$c; $c=substr($st,$i,1);$i++; next;}
  if ($s==4 && $c=~/"/) {   $t="S\t$t'";push(@tokens,$t); $c=substr($st,$i,1);$i++; $s=0; next;}
  if ($s==0 ) {  $t="P\t$c";push(@tokens,$t); $c=substr($st,$i,1);$i++; $s=0; next;}
 }
 return @tokens;

}
#-----------------------------------------------------------------------------
sub punctuation {
  local($punct) = '';
  foreach $t (@_) {
    ($type,$value) = split("\t",$t);
    if ($type eq 'P') { $punct.=$value;}
  }
  return $punct;
}
#--------------------------------------------------------------------------------------
sub loadbnf {
 # build dictionary of terms and parsed definitions
 local(@lines) = @_;
 local($line,$term,@terms);
 foreach $line (@lines) { 			# for each input line
   if ($line =~/^#/) {next; }                    # ignore comments
   @tokens = &tokenize($line);
   $punct = &punctuation(@tokens);
#   print @tokens; print ">>$punct \n";
   if ($punct =~ /^=#?$/) { # sequence
      &parseSeq;
   }
   elsif ($punct =~ /^=\|+#?$/) { # selection
      &parseSel;}
   elsif($punct =~ /^=\{\}#?$/) { # iteration
      &parseItr;}
   else { 
#      print "$line was invalid<br>\n";
      }
#   print "<<$term $rule{$term}  $terms{$term} $dist{$term}\n";
   push(@orderedTerms,$term);
 }
 
}
#-------------------------------------------------------------------------
sub parseSel {
   ($type,$value) = split("\t",shift(@tokens));
   if ($type ne 'W') { next;}
   $term = $value; @terms=();
   $rule{$term}='sel';
   ($type,$value) = split("\t",shift(@tokens)); #=
   ($type,$value) = split("\t",shift(@tokens)); 
   while (($type eq "W") || ($type eq "S")) {
     push(@terms,$value);   
     ($type,$value) = split("\t",shift(@tokens)); 
     if ($value eq '#') {last;}
     ($type,$value) = split("\t",shift(@tokens)); # |
   }
   $terms{$term} = join("\t",@terms);
   if ($type eq 'P') {
     ($type,$value) = split("\t",shift(@tokens)); 
     $dist{$term}=$value; 

   }
}
#-------------------------------------------------------------------------
sub parseSeq {
   ($type,$value) = split("\t",shift(@tokens));
   if ($type ne 'W') { next;}
   $term = $value; @terms=();
   $rule{$term}='seq';
   ($type,$value) = split("\t",shift(@tokens)); #=
   ($type,$value) = split("\t",shift(@tokens)); 
   while (($type eq "W") || ($type eq "S")) {
     push(@terms,$value);   
     ($type,$value) = split("\t",shift(@tokens)); 
   }
   $terms{$term} = join("\t",@terms);
   if ($type eq 'P') {
     ($type,$value) = split("\t",shift(@tokens)); 
     $dist{$term}=$value; 

   }
}
#-------------------------------------------------------------------------
sub parseItr {
   ($type,$value) = split("\t",shift(@tokens));
   if ($type ne 'W') { next;}
   $term = $value; @terms=(); $min = 0; 
   $rule{$term}='itr';
   ($type,$value) = split("\t",shift(@tokens)); #=
   ($type,$value) = split("\t",shift(@tokens)); #=
   if ($type eq 'N') {
      $min=$value;
      ($type,$value) = split("\t",shift(@tokens)); 
   }
   ($type,$value) = split("\t",shift(@tokens)); # {
   $terms{$term} = $value;
   ($type,$value) = split("\t",shift(@tokens)); # }
   ($type,$value) = split("\t",shift(@tokens)); 
   if ($type eq 'N') {
      $max=$value;
      ($type,$value) = split("\t",shift(@tokens)); 
   }
   if ($type eq 'P') {
     ($type,$value) = split("\t",shift(@tokens)); # # 
     $dtype = $value; 
     ($type,$value) = split("\t",shift(@tokens)); 
     $dist{$term}="$dtype $value $min";
   }
   elsif ($max) {
     $dist{$term}="U $min $max";
   }
   else {
     $dist{$term}="B $PROB $min";
   } 
}



#---------------------------------------------------------------------------------------------
sub expand  {
    local ($term)  = $_[0];
    if ($term =~ /^'(.*)'$/)  {   # a terminal
	if ($1 =~ /^\\/) {
	    if ($1 eq '\n') {print "\n";}
	    if ($1 eq '\t') {print "\t";}
	}
        else {
           print $1;
           print $SEP;
       }
	}
    elsif ($term eq '-')  { }  # null so ignore

    else {  # a non-terminal
	    &expandDef($term);
	}
    }
#---------------------------------------------------------------------------------------------
 sub expandDef {
    local ($def)= shift(@_);
    local ($type, @terms, $RHS);
    if ($RHS=$terms{$term}) { 
	@terms = split("\t",$RHS);
	$type = $rule{$term};
	if ($type eq 'sel') {  &selection($def, @terms);
		     }
	elsif  ( $type eq 'itr') { &iterate($def, @terms);
			  }
	elsif  ($type eq 'seq') { &sequence($def, @terms);
			 }
	elsif  ($type eq 'opt') { &optional($def, @terms);
			 }
	else { die "undefined $type\n";
	  }
    }
    else { print "$term"; print $SEP;} 
}
#---------------------------------------------------------------------------------------------
 sub selection {
     local ($def) = shift(@_);
     local ($n, $max,$dist,$term);
     $max = @_;
     if ($dist = $dist{$def}) {
        if ($dist =~ m/Z/)  # Zipfs
          {$n = &zipf($max);}
        }
     else  {   # uniform
        $n = &uniform(0,$max-1);
        }
     $term = @_[$n];
     &expand ($term);
 }
#---------------------------------------------------------------------------------------------
 sub optional {
     local ($def) = shift(@_);
     local ( $term);
     if (rand(1) <= $PROB ) {
        &expand ($term);
        }
 }

#---------------------------------------------------------------------------------------------
 sub sequence {
     local ($def) = shift(@_);
     local ($term); 
     foreach $term (@_) {
       &expand ($term);
       }
 }

#---------------------------------------------------------------------------------------------
sub iterate {
     local ($def) = shift(@_);
     local ($term) = shift(@_);
     local ($n, $i, $d, $dtype, $p1, $p2); 
     if ($d = $dist{$def} )  {
         ($dtype,$p1,$p2) = split(/\s+/,$d);
# print "<<<< DEF=$def Term=$term Dist=$d Type=$dtype P1=$p1 P2=$p2 \n";
         if ($dtype eq 'U' ) {
                 $n = &uniform($p1,$p2);
                 for ($i=1;$i<=$n;$i++) {
	             &expand($term );
                 }
             }
        elsif ($dtype eq 'B' ) {
            for ($i=1;$i<=$p2;$i++)  { &expand($term); }
            while ( rand(1) <= $p1 ) { &expand($term); }
            }
        else {die " unrecognised distribution $d";}
     }   
}

#---------------------------------------------------------------------------------------------
sub printbnf {
  foreach $term( @orderedTerms) {
      print "$term = [ $rule{$term} ] ";
      @terms = split ("\t",$terms{$term});
      foreach $t (@terms) { print "$t "; }
      if ($dist = $dist{$term}) {
          print "   # $dist\n";
          }
      else {print "\n";}
  }
}


#---------------------------------------------------------------------------------------------
sub uniform {
  local ($min) = $_[0];
  local ($max) = $_[1];
  $n = $min + int(rand($max - $min + 1));
#print "#U $min $max $n\n";
  return $n;
}

#---------------------------------------------------------------------------------------------
sub trim {
   local ($string) = $_[0];
    $string =~ s/^\s+//;                          # trim blanks fore and aft
    $string =~ s/\s+$//;  
    return $string;
}

#---------------------------------------------------------------------------------------------
sub zipf {
   local ($n) = shift(@_);
   local ($cdf, @cdf);
   local ($i, $Hn);
   if ( ! $zipf[$n] ) {  # not yet defined for size n
      $Hn = 0;
      for ($i=1; $i<=$n; $i++) { $Hn += 1/$i;}
      $cdf[0] = 0;
      for($i=1;$i<=$n;$i++) {
         $cdf[$i] = $cdf[$i-1] + 1 / ($Hn * $i);
      }
      $zipf[$n] = join(':',@cdf) ;
#      print "Zipfs $n is @cdf\n";
      }
    $cdf = $zipf[$n];
    @cdf = split(':',$cdf);
    $r = rand(1);
    for ($i = 1; $r > $cdf[$i]; $i++) {};
    return $i-1;
}
 
