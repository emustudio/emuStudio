package net.sf.emustudio.devices.mits88disk.cpmfs;

class TrackAndSector {
  public int track;
  public int sector;

  TrackAndSector(int track, int sector) {
    this.track = track;
    this.sector = sector;
  }

  @Override
  public String toString() {
    return "T=" + track + " S=" + sector;
  }
}
