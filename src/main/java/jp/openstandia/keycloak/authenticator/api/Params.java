public interface Params {
  public Map<String, String> data;
  public void setAttribute(String key, String value);
  public String toJSON();
}