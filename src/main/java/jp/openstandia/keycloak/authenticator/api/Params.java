package jp.openstandia.keycloak.authenticator.api;

import java.util.Map;

public interface Params {
  void setAttribute(String key, String value);
  String toJSON();
Map<String, String> toMap();
}