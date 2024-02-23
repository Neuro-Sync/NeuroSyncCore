package headset.events.nskAlgo.algoStateChange;

public enum AlgoState {
  //TODO: REMOVE TEST
  TEST(-1),
  INITED(256),
  RUNNING(512),
  COLLECTING_BASELINE_DATA(768),
  STOP(1024),
  PAUSE(1280),
  UNINTIED(1536),
  ANALYSING_BULK_DATA(2048),
  MASK(65280);
  private int value;

  AlgoState(int value) {
    this.setValue(value);
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }
}
