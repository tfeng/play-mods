/**
 * Copyright 2015 Thomas Feng
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.tfeng.playmods.oauth2;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class OAuth2JsonConverter {

  private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

  private static final String STRING_ACCOUNT_NON_EXPIRED = "accountNonExpired";

  private static final String STRING_ACCOUNT_NON_LOCKED = "accountNonLocked";

  private static final String STRING_APPROVED = "approved";

  private static final String STRING_AUTHENTICATED = "authenticated";

  private static final String STRING_AUTHENTICATION = "authentication";

  private static final String STRING_AUTHORITIES = "authorities";

  private static final String STRING_CLIENT_ID = "clientId";

  private static final String STRING_CREDENTIALS = "credentials";

  private static final String STRING_CREDENTIALS_NON_EXPIRED = "credentialsNonExpired";

  private static final String STRING_DATA = "data";

  private static final String STRING_ENABLED = "enabled";

  private static final String STRING_EXPIRATION = "expiration";

  private static final String STRING_KEY = "key";

  private static final String STRING_OAUTH2 = "oauth2";

  private static final String STRING_PASSWORD = "password";

  private static final String STRING_PRINCIPAL = "principal";

  private static final String STRING_REDIRECT_URI = "redirectUri";

  private static final String STRING_REFRESH_TOKEN = "refreshToken";

  private static final String STRING_REQUEST = "request";

  private static final String STRING_REQUEST_PARAMETERS = "requestParameters";

  private static final String STRING_RESOURCE_IDS = "resourceIds";

  private static final String STRING_RESPONSE_TYPES = "responseTypes";

  private static final String STRING_SCOPE = "scope";

  private static final String STRING_TOKEN_TYPE = "tokenType";

  private static final String STRING_TYPE = "type";

  private static final String STRING_USERNAME = "username";

  private static final String STRING_USERNAME_PASSWORD = "usernamePassword";

  private static final String STRING_VALUE = "value";

  public static JsonNode accessTokenToJson(OAuth2AccessToken token) {
    if (token == null) {
      return null;
    }
    ObjectNode node = FACTORY.objectNode();
    if (token.getExpiration() != null) {
      node.set(STRING_EXPIRATION, FACTORY.numberNode(token.getExpiration().getTime()));
    }
    node.set(STRING_REFRESH_TOKEN, refreshTokenToJson(token.getRefreshToken()));
    node.set(STRING_SCOPE, stringsToArray(token.getScope()));
    node.set(STRING_TOKEN_TYPE, FACTORY.textNode(token.getTokenType()));
    node.set(STRING_VALUE, FACTORY.textNode(token.getValue()));
    return node;
  }

  public static JsonNode authenticationToJson(OAuth2Authentication authentication) {
    if (authentication == null) {
      return null;
    }
    ObjectNode node = FACTORY.objectNode();
    node.set(STRING_AUTHENTICATION, genericAuthenticationToJson(authentication.getUserAuthentication()));
    node.set(STRING_REQUEST, requestToJson(authentication.getOAuth2Request()));
    return node;
  }

  public static JsonNode genericAuthenticationToJson(Authentication authentication) {
    if (authentication == null) {
      return null;
    }
    ObjectNode node = FACTORY.objectNode();
    if (authentication instanceof OAuth2Authentication) {
      node.set(STRING_DATA, authenticationToJson((OAuth2Authentication) authentication));
      node.set(STRING_TYPE, FACTORY.textNode(STRING_OAUTH2));
    } else if (authentication instanceof UsernamePasswordAuthenticationToken) {
      node.set(STRING_DATA,
          usernamePasswordAuthenticationTokenToJson((UsernamePasswordAuthenticationToken) authentication));
      node.set(STRING_TYPE, FACTORY.textNode(STRING_USERNAME_PASSWORD));
    } else {
      throw new RuntimeException("Unable to convert authentication type " + authentication.getClass().getName()
          + " to JSON");
    }
    return node;
  }

  public static OAuth2AccessToken jsonToAccessToken(JsonNode json) {
    if (json == null || json.getNodeType() == JsonNodeType.NULL) {
      return null;
    }
    ObjectNode node = (ObjectNode) json;
    DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(getText(node, STRING_VALUE));
    JsonNode expiration = node.get(STRING_EXPIRATION);
    if (expiration != null) {
      token.setExpiration(new Date(expiration.asLong()));
    }
    token.setRefreshToken(jsonToRefreshToken(node.get(STRING_REFRESH_TOKEN)));
    token.setScope(Sets.newHashSet(arrayToStrings(node.get(STRING_SCOPE))));
    token.setTokenType(getText(node, STRING_TOKEN_TYPE));
    return token;
  }

  public static OAuth2Authentication jsonToAuthentication(JsonNode json) {
    if (json == null || json.getNodeType() == JsonNodeType.NULL) {
      return null;
    }
    ObjectNode node = (ObjectNode) json;
    Authentication userAuthentication = jsonToGenericAuthentication(node.get(STRING_AUTHENTICATION));
    OAuth2Request request = jsonToRequest(node.get(STRING_REQUEST));
    OAuth2Authentication authentication = new OAuth2Authentication(request, userAuthentication);
    return authentication;
  }

  public static Authentication jsonToGenericAuthentication(JsonNode json) {
    if (json == null || json.getNodeType() == JsonNodeType.NULL) {
      return null;
    }
    ObjectNode node = (ObjectNode) json;
    String type = getText(node, STRING_TYPE);
    if (STRING_OAUTH2.equals(type)) {
      return jsonToAuthentication(json.get(STRING_DATA));
    } else if (STRING_USERNAME_PASSWORD.equals(type)) {
      return jsonToUsernamePasswordAuthentication(json.get(STRING_DATA));
    } else {
      throw new RuntimeException("Unknown authentication type " + type);
    }
  }

  public static OAuth2RefreshToken jsonToRefreshToken(JsonNode json) {
    if (json == null || json.getNodeType() == JsonNodeType.NULL) {
      return null;
    }
    ObjectNode node = (ObjectNode) json;
    String value = getText(node, STRING_VALUE);
    JsonNode expiration = node.get(STRING_EXPIRATION);
    OAuth2RefreshToken refreshToken;
    if (expiration == null) {
      refreshToken = new DefaultOAuth2RefreshToken(value);
    } else {
      refreshToken = new DefaultExpiringOAuth2RefreshToken(value, new Date(expiration.asLong()));
    }
    return refreshToken;
  }

  public static OAuth2Request jsonToRequest(JsonNode json) {
    if (json == null || json.getNodeType() == JsonNodeType.NULL) {
      return null;
    }
    ObjectNode node = (ObjectNode) json;
    boolean approved = node.get(STRING_APPROVED).booleanValue();
    Set<GrantedAuthority> authorities = Sets.newHashSet(arrayToGrantedAuthorities(node.get(STRING_AUTHORITIES)));
    String clientId = getText(node, STRING_CLIENT_ID);
    String redirectUri = getText(node, STRING_REDIRECT_URI);
    Map<String, String> requestParameters = arrayToStringMap(node.get(STRING_REQUEST_PARAMETERS));
    Set<String> resourceIds = Sets.newHashSet(arrayToStrings(node.get(STRING_RESOURCE_IDS)));
    Set<String> responseTypes = Sets.newHashSet(arrayToStrings(node.get(STRING_RESPONSE_TYPES)));
    Set<String> scope = Sets.newHashSet(arrayToStrings(node.get(STRING_SCOPE)));
    OAuth2Request request = new OAuth2Request(requestParameters, clientId, authorities, approved, scope, resourceIds,
        redirectUri, responseTypes, null);
    return request;
  }

  public static UserDetails jsonToUserDetails(JsonNode json) {
    if (json == null || json.getNodeType() == JsonNodeType.NULL) {
      return null;
    }
    ObjectNode node = (ObjectNode) json;
    UserDetails userDetails = new User(getText(node, STRING_USERNAME), getText(node, STRING_PASSWORD),
        node.get(STRING_ENABLED).booleanValue(), node.get(STRING_ACCOUNT_NON_EXPIRED).booleanValue(),
        node.get(STRING_CREDENTIALS_NON_EXPIRED).booleanValue(), node.get(STRING_ACCOUNT_NON_LOCKED).booleanValue(),
        arrayToGrantedAuthorities(node.get(STRING_AUTHORITIES)));
    return userDetails;
  }

  public static UsernamePasswordAuthenticationToken jsonToUsernamePasswordAuthentication(JsonNode json) {
    if (json == null || json.getNodeType() == JsonNodeType.NULL) {
      return null;
    }
    ObjectNode node = (ObjectNode) json;
    UserDetails userDetails = jsonToUserDetails(node.get(STRING_PRINCIPAL));
    String credentials = getText(node, STRING_CREDENTIALS);
    List<GrantedAuthority> authorities = arrayToGrantedAuthorities(node.get(STRING_AUTHORITIES));
    UsernamePasswordAuthenticationToken token;
    if (node.get(STRING_AUTHENTICATED).asBoolean()) {
      token = new UsernamePasswordAuthenticationToken(userDetails, credentials, authorities);
    } else {
      token = new UsernamePasswordAuthenticationToken(userDetails, credentials);
    }
    return token;
  }

  public static JsonNode refreshTokenToJson(OAuth2RefreshToken token) {
    if (token == null) {
      return null;
    }
    ObjectNode node = FACTORY.objectNode();
    node.set(STRING_VALUE, FACTORY.textNode(token.getValue()));
    if (token instanceof ExpiringOAuth2RefreshToken) {
      Date expiration = ((ExpiringOAuth2RefreshToken) token).getExpiration();
      if (expiration != null) {
        node.set(STRING_EXPIRATION, FACTORY.numberNode(expiration.getTime()));
      }
    }
    return node;
  }

  public static JsonNode requestToJson(OAuth2Request request) {
    if (request == null) {
      return null;
    }
    ObjectNode node = FACTORY.objectNode();
    node.set(STRING_APPROVED, FACTORY.booleanNode(request.isApproved()));
    node.set(STRING_AUTHORITIES, grantedAuthoritiesToArray(request.getAuthorities()));
    node.set(STRING_CLIENT_ID, FACTORY.textNode(request.getClientId()));
    node.set(STRING_REDIRECT_URI, FACTORY.textNode(request.getRedirectUri()));
    node.set(STRING_REQUEST_PARAMETERS, stringMapToArray(request.getRequestParameters()));
    node.set(STRING_RESOURCE_IDS, stringsToArray(request.getResourceIds()));
    node.set(STRING_RESPONSE_TYPES, stringsToArray(request.getResponseTypes()));
    node.set(STRING_SCOPE, stringsToArray(request.getScope()));
    return node;
  }

  public static JsonNode userDetailsToJson(UserDetails userDetails) {
    if (userDetails == null) {
      return null;
    }
    ObjectNode node = FACTORY.objectNode();
    node.set(STRING_ACCOUNT_NON_EXPIRED, FACTORY.booleanNode(userDetails.isAccountNonExpired()));
    node.set(STRING_ACCOUNT_NON_LOCKED, FACTORY.booleanNode(userDetails.isAccountNonLocked()));
    node.set(STRING_AUTHORITIES, grantedAuthoritiesToArray(userDetails.getAuthorities()));
    node.set(STRING_CREDENTIALS_NON_EXPIRED, FACTORY.booleanNode(userDetails.isCredentialsNonExpired()));
    node.set(STRING_ENABLED, FACTORY.booleanNode(userDetails.isEnabled()));
    node.set(STRING_PASSWORD, FACTORY.textNode(userDetails.getPassword()));
    node.set(STRING_USERNAME, FACTORY.textNode(userDetails.getUsername()));
    return node;
  }

  public static JsonNode usernamePasswordAuthenticationTokenToJson(UsernamePasswordAuthenticationToken token) {
    if (token == null) {
      return null;
    }
    ObjectNode node = FACTORY.objectNode();
    UserDetails userDetails = (UserDetails) token.getPrincipal();
    node.set(STRING_AUTHENTICATED, FACTORY.booleanNode(token.isAuthenticated()));
    node.set(STRING_PRINCIPAL, userDetailsToJson(userDetails));
    node.set(STRING_CREDENTIALS, FACTORY.textNode((String) token.getCredentials()));
    if (token.isAuthenticated()) {
      node.set(STRING_AUTHORITIES, grantedAuthoritiesToArray(token.getAuthorities()));
    }
    return node;
  }

  private static List<GrantedAuthority> arrayToGrantedAuthorities(JsonNode node) {
    if (node == null || node.getNodeType() == JsonNodeType.NULL) {
      return null;
    }
    List<GrantedAuthority> list = Lists.newArrayListWithCapacity(node.size());
    node.elements().forEachRemaining(e -> list.add(new SimpleGrantedAuthority(e.asText())));
    return list;
  }

  private static Map<String, String> arrayToStringMap(JsonNode node) {
    if (node == null || node.getNodeType() == JsonNodeType.NULL) {
      return null;
    }
    Map<String, String> map = Maps.newHashMapWithExpectedSize(node.size());
    node.forEach(e -> {
      ObjectNode objectNode = (ObjectNode) e;
      String value = getText(objectNode, STRING_VALUE);
      String key = getText(objectNode, STRING_KEY);
      if (value != null) {
        map.put(key, value);
      }
    });
    return map;
  }

  private static List<String> arrayToStrings(JsonNode node) {
    if (node == null || node.getNodeType() == JsonNodeType.NULL) {
      return null;
    }
    List<String> list = Lists.newArrayListWithCapacity(node.size());
    node.elements().forEachRemaining(e -> list.add(e.asText()));
    return list;
  }

  private static String getText(ObjectNode node, String fieldName) {
    JsonNode textNode = node.get(fieldName);
    return textNode == null ? null : textNode.asText();
  }

  private static ArrayNode grantedAuthoritiesToArray(Collection<? extends GrantedAuthority> grantedAuthorities) {
    if (grantedAuthorities == null) {
      return null;
    }
    ArrayNode node = FACTORY.arrayNode();
    for (GrantedAuthority grantedAuthority : grantedAuthorities) {
      node.add(FACTORY.textNode(grantedAuthority.getAuthority()));
    }
    return node;
  }

  private static ArrayNode stringMapToArray(Map<String, String> map) {
    if (map == null) {
      return null;
    }
    ArrayNode node = FACTORY.arrayNode();
    map.forEach((key, value) -> {
      if (value != null) {
        ObjectNode objectNode = FACTORY.objectNode();
        objectNode.set(STRING_KEY, FACTORY.textNode(key));
        objectNode.set(STRING_VALUE, FACTORY.textNode(value));
        node.add(objectNode);
      }
    });
    return node;
  }

  private static ArrayNode stringsToArray(Collection<String> strings) {
    if (strings == null) {
      return null;
    }
    ArrayNode node = FACTORY.arrayNode();
    for (String s : strings) {
      node.add(FACTORY.textNode(s));
    }
    return node;
  }
}
