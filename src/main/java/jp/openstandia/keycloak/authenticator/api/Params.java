package jp.openstandia.keycloak.authenticator.api;

public interface Params {
  void setAttribute(String key, String value);
  String toJSON();
}