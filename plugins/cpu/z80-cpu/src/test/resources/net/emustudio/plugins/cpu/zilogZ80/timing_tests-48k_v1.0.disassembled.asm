; Logical as-z80 disassembly of timing_tests-48k_v1.0
;
; Notes:
; - Original tape contains BASIC UI/loader plus data-driven timing harness.
; - Binary image in timing_tests-48k_v1.0.bin is direct RAM payload loaded at 0xC000.
; - Raw extracted image is kept in timing_tests-48k_v1.0.asm as db-oriented dump.
; - This file reconstructs executable menu bodies from opcode tables used by the
;   harness. It intentionally omits display, keyboard, and contention plumbing.
;
; The unit tests in TimingTests48kProgramTest mirror these bodies and assert the
; CPU-core effects directly.

org 0xc000

menu01:
  jr menu01_after
menu01_after:
  inc bc
  ld bc, (0x4000)
  ld (0x4002), bc

menu02:
  inc bc
  dec bc
  inc de
  dec de
  inc hl
  dec hl
  inc sp
  dec sp

menu03:
  nop
  ld b, b
  inc b
  dec b

menu04:
  add a, b
  adc a, b
  sub b
  sbc a, b
  and b
  xor b
  or b
  cp b
  add a, (hl)
  adc a, (hl)
  sub (hl)
  sbc a, (hl)
  and (hl)
  xor (hl)
  or (hl)
  cp (hl)

menu05:
  exx
  ex af, af'
  ex de, hl

menu06:
  daa
  cpl
  ccf
  scf

menu07:
  push bc
  pop bc
  ex (sp), hl

menu08:
  rla
  rra
  rlca
  rrca
  rld
  rrd

menu09:
  ld hl, 0x1234
  ld hl, (0x4000)
  ld (0x4002), hl
  ld (hl), 0x20

; Menu 10 and 11 use same forms under different incoming flag states.
menu10_push_bc:
  push bc

menu10_pop_bc:
  pop bc

menu10_call_nz:
  call nz, 0x1234

menu10_call_z:
  call z, 0x1234

menu10_jp_nz:
  jp nz, 0x1234

menu10_jp_z:
  jp z, 0x1234

menu11_call_z:
  call z, 0x1234

menu11_call_nz:
  call nz, 0x1234

menu11_jp_z:
  jp z, 0x1234

menu11_jp_nz:
  jp nz, 0x1234

menu12:
  rlc b
  rrc c
  rl d
  rr e
  sla h
  sra l
  srl c
  rlc (hl)
  rrc (hl)
  rl (hl)
  rr (hl)
  sla (hl)
  sra (hl)
  srl (hl)

menu13:
  bit 5, h
  bit 7, c
  bit 0, d
  bit 7, e

menu14:
  set 0, d
  res 0, d
  set 7, a
  res 7, a

menu15:
  im 0
  im 1
  im 2
  ld a, i
  ld i, a
  ld r, a
  ld a, r

menu16:
  ld sp, hl
  add hl, bc
  adc hl, bc
  sbc hl, bc

menu17:
  add a, b
  add a, c
  add a, d
  add a, e

menu18:
  add a, h
  add a, l
  add a, (hl)
  add a, a

menu19:
  ld b, 0x12
  ld bc, 0x1234
  ld a, (0x4000)
  ld (0x4002), a
  ld bc, (0x4004)
  ld (0x4006), bc

menu20:
  ld a, (bc)
  ld a, (de)
  ld (bc), a
  ld (de), a

menu21:
  push ix
  pop ix
  ld (ix+0), b
  ld (ix+0), 0x12

menu22:
  ld a, (ix+0)
  ld b, (ix+0)
  ld a, (iy+0)
  ld b, (iy+0)

menu23:
  ld a, (ix+0)
  ld b, (ix+0)
  bit 0, (ix+0)
  res 0, (ix+0)
  set 0, (ix+0)

menu24:
  ld a, (iy+0)
  ld b, (iy+0)
  bit 0, (iy+0)
  res 0, (iy+0)
  set 0, (iy+0)

menu25:
  add a, (ix+0)
  adc a, (ix+0)
  sub (ix+0)
  sbc a, (ix+0)
  and (ix+0)
  xor (ix+0)
  or (ix+0)
  cp (ix+0)

menu26:
  add a, (iy+0)
  adc a, (iy+0)
  sub (iy+0)
  sbc a, (iy+0)
  and (iy+0)
  xor (iy+0)
  or (iy+0)
  cp (iy+0)

menu27:
  bit 0, (hl)
  res 0, (hl)
  set 0, (hl)
  inc (hl)
  dec (hl)

menu28_ret:
  ret

menu28_ret_z:
  ret z

menu28_ret_nz:
  ret nz

menu28_reti:
  reti

menu28_retn:
  retn

menu29_call_nz:
  call nz, 0x1234

menu29_call_z:
  call z, 0x1234

menu29_jr_nz:
  jr nz, menu29_jr_nz_after
menu29_jr_nz_after:

menu29_jr_z:
  jr z, menu29_jr_z_after
menu29_jr_z_after:

menu29_djnz_repeat:
  djnz menu29_djnz_repeat_after
menu29_djnz_repeat_after:

menu29_djnz_final:
  djnz menu29_djnz_final_after
menu29_djnz_final_after:

; Menus 30-33 reuse same opcode with different BC/setup to hit repeat/final paths.
menu30_ldi:
  ldi

menu30_ldir:
  ldir

menu30_ldd:
  ldd

menu30_lddr:
  lddr

menu31_cpi:
  cpi

menu31_cpir:
  cpir

menu31_cpd:
  cpd

menu31_cpdr:
  cpdr

menu32_ini:
  ini

menu32_inir:
  inir

menu32_ind:
  ind

menu32_indr:
  indr

menu33_outi:
  outi

menu33_otir:
  otir

menu33_outd:
  outd

menu33_otdr:
  otdr

menu34:
  rst 0x18

; Original menus 35-37 differ only by Spectrum-visible contention and display state.
; CPU-core sequence is same for all three, so one body is enough for logic extraction.
menu35_37_cpu_only:
  in a, (0x00)
  out (0x00), a
  in a, (c)
  out (c), b
