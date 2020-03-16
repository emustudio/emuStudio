prologues:=3;
outputtemplate:="png/%j-%c.png";
outputformat:="png";

def computer(expr unit, pos_x, pos_y, color, discolor, s) =
  u:= unit; % 24 = 24 bp = 1/3inch
  pen_scale:= 2/11u;
  pen_scale_min:= 1/7u;

  w_desk:= 7/2u;
  h_desk:= u;

  w_disp:= 2u;
  disp_x:= pos_x + (w_desk - w_disp) / 2;
  disp_y:= pos_y + h_desk + 1.2/3u ;

  w_min_disp:= w_disp - 4 * pen_scale;
  min_disp_x:= disp_x + 2 * pen_scale;
  min_disp_y:= disp_y + 2 * pen_scale;

  dots_scale:= 2.1/10u;
  dots_x:= pos_x + pen_scale + pen_scale_min;
  dots_y:= pos_y + h_desk/2 - 3 * dots_scale / 2 + 1.5*pen_scale_min/2; 

  floppy_size:= 5/6u;
  floppy_x:= pos_x + w_desk - floppy_size - 3*pen_scale_min;
  floppy_y:= pos_y + h_desk / 2 + pen_scale_min/2;

  % display
  draw unitsquare 
    scaled w_disp
    shifted (disp_x,disp_y)
    withpen pencircle scaled pen_scale
    withcolor color
  ;

  if s>0:
     fill unitsquare
       scaled w_min_disp
       shifted (min_disp_x,min_disp_y)
       withpen pencircle scaled pen_scale
       withcolor discolor
     ;
  fi;

  % case
  draw unitsquare
    xscaled w_desk
    yscaled h_desk
    shifted (pos_x,pos_y)
    withpen pencircle scaled pen_scale
    withcolor color
  ;

  % case details
  for i=0 upto 2:
    for j=0 upto 2:
      draw (i,j)
        scaled dots_scale
        shifted (dots_x,dots_y)
        withpen pencircle scaled pen_scale_min
        withcolor color
      ;
    endfor;
  endfor;

  % floppy
  draw (floppy_x,floppy_y)--(floppy_x + floppy_size,floppy_y)
    withpen pensquare scaled pen_scale_min
    withcolor color
  ;
enddef;

beginfig(1);
  u:=80;
  um:=25;
  
  computer(u,0,0,black, white,1);
  computer(um,96, 145,(1,215/255,0),black,1);
endfig;
beginfig(2);
  computer(8,1,0.5, (1,215/255,0), white, 1); % (102/255, 153/255, 255/255));
  % computer(2.5,1+9.55,0.5+14.5,black, black, 1);
endfig;
beginfig(3);
  u:=20;
  um:=6.25;

  computer(u,0,0,black, black,0);
  computer(um,96/2/2, 145/2/2, black, black,1);
endfig;

end
