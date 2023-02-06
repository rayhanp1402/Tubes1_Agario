package Enums;

public enum PlayerActions { 
  // untuk menghindari kesalahan, nama nya diganti
  Forward(1),
  Stop(2),
  StartAfterBurner(3),
  StopAfterBurner(4),
  FireTorpedoes(5),
  FireSupernova(6),
  DetonateSupernova(7),
  FireTeleport(8),
  Teleport(9),
  ActivateShield(10);

  public final Integer value;

  private PlayerActions(Integer value) {
    this.value = value;
  }
}
