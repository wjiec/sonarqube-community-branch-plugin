/*
 * Copyright (C) 2020 Michael Clarke
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.RepositoryAuthenticationToken;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RestApplicationAuthenticationProviderTest {

    @Test
    public void testTokenRetrievedHappyPath() throws IOException {
        UrlConnectionProvider urlProvider = mock(UrlConnectionProvider.class);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(123456789L), ZoneId.of("UTC"));

        String expectedAuthenticationToken = "expected authentication token";
        String projectPath = "project path";
        String expectedRepositoryId = "expected repository Id";

        URLConnection installationsUrlConnection = mock(URLConnection.class);
        doReturn(new ByteArrayInputStream(
                "[{\"repositories_url\": \"repositories_url\", \"access_tokens_url\": \"tokens_url\"}]"
                        .getBytes(StandardCharsets.UTF_8))).when(installationsUrlConnection).getInputStream();

        HttpURLConnection accessTokensUrlConnection = mock(HttpURLConnection.class);
        doReturn(new ByteArrayInputStream(
                ("{\"token\": \"" + expectedAuthenticationToken + "\"}").getBytes(StandardCharsets.UTF_8)))
                .when(accessTokensUrlConnection).getInputStream();
        doReturn(accessTokensUrlConnection).when(urlProvider).createUrlConnection("tokens_url");


        HttpURLConnection repositoriesUrlConnection = mock(HttpURLConnection.class);
        doReturn(new ByteArrayInputStream(
                ("{\"repositories\": [{\"node_id\": \"" + expectedRepositoryId + "\", \"full_name\": \"" + projectPath +
                 "\"}]}").getBytes(StandardCharsets.UTF_8))).when(repositoriesUrlConnection).getInputStream();
        doReturn(repositoriesUrlConnection).when(urlProvider).createUrlConnection("repositories_url");

        String apiUrl = "apiUrl";
        doReturn(installationsUrlConnection).when(urlProvider).createUrlConnection(eq(apiUrl + "/app/installations"));

        String appId = "appID";

        String apiPrivateKey;
        try (InputStream inputStream = getClass().getResourceAsStream("/rsa-private-key.pem")) {
            apiPrivateKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        RestApplicationAuthenticationProvider testCase = new RestApplicationAuthenticationProvider(clock, urlProvider);
        RepositoryAuthenticationToken result = testCase.getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath);

        assertEquals(expectedAuthenticationToken, result.getAuthenticationToken());
        assertEquals(expectedRepositoryId, result.getRepositoryId());

        ArgumentCaptor<String> requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(installationsUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(accessTokensUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(accessTokensUrlConnection).setRequestMethod("POST");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(repositoriesUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(repositoriesUrlConnection).setRequestMethod("GET");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer " + expectedAuthenticationToken),
                     requestPropertyArgumentCaptor.getAllValues());
    }

    @Test
    public void testTokenRetrievedPaginatedHappyPath() throws IOException {
        UrlConnectionProvider urlProvider = mock(UrlConnectionProvider.class);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(123456789L), ZoneId.of("UTC"));

        String expectedAuthenticationToken = "expected authentication token";
        String projectPath = "project path";
        String expectedRepositoryId = "expected repository Id";

        URLConnection installationsUrlConnection = mock(URLConnection.class);
        doReturn(new ByteArrayInputStream(
                "[{\"repositories_url\": \"repositories_url\", \"access_tokens_url\": \"tokens_url\"}]"
                        .getBytes(StandardCharsets.UTF_8))).when(installationsUrlConnection).getInputStream();

        HttpURLConnection accessTokensUrlConnection = mock(HttpURLConnection.class);
        doReturn(new ByteArrayInputStream(
                ("{\"token\": \"" + expectedAuthenticationToken + "\"}").getBytes(StandardCharsets.UTF_8)))
                .when(accessTokensUrlConnection).getInputStream();
        doReturn(accessTokensUrlConnection).when(urlProvider).createUrlConnection("tokens_url");


        for (int i = 0; i < 5; i ++) {
            HttpURLConnection repositoriesUrlConnection = mock(HttpURLConnection.class);
            doReturn(new ByteArrayInputStream(
                    ("{\"repositories\": [{\"node_id\": \"" + expectedRepositoryId + "a\", \"full_name\": \"" +
                     projectPath + "a\"}]}").getBytes(StandardCharsets.UTF_8))).when(repositoriesUrlConnection).getInputStream();
            String link;
            switch (i) {
                case 0:
                    link = "https://dummy.url/path?param=dummy&page=6; rel=\"last\", <https://dummy.url/path?param=dummy&page=2>; rel=\"next\"";
                    break;
                case 1:
                    link = "<https://dummy.url/path?param=dummy&page=6>; xxx=\"last\",<https://dummy.url/path?param=dummy&page=3>; rel=\"next\"";
                    break;
                case 2:
                    link = "<https://dummy.url/path?param=dummy&page=4>; rel=next";
                    break;
                case 3:
                    link = "<https://dummy.url/path?param=dummy&page=6>; rel, <https://dummy.url/path?param=dummy&page=5>; rel=\"next\"";
                    break;
                case 4:
                    link = "<https://dummy.url/path?param=dummy&page=6>; rel=\"next\", <https://dummy.url/path?param=dummy&page=6>; rel=\"last\"";
                    break;
                default:
                    throw new IllegalStateException("unknown link");

            }
            doReturn(link).when(repositoriesUrlConnection).getHeaderField("Link");
            doReturn(repositoriesUrlConnection).when(urlProvider).createUrlConnection(i == 0 ? "repositories_url" : "https://dummy.url/path?param=dummy&page=" + (i + 1));
        }

        HttpURLConnection repositoriesUrlConnection2 = mock(HttpURLConnection.class);
        doReturn(new ByteArrayInputStream(
                ("{\"repositories\": [{\"node_id\": \"" + expectedRepositoryId + "\", \"full_name\": \"" + projectPath +
                 "\"}]}").getBytes(StandardCharsets.UTF_8))).when(repositoriesUrlConnection2).getInputStream();
        doReturn("<https://dummy.url/path?param=dummy&page=3>; rel=\"last\", <https://dummy.url/path?param=dummy&page=3>; rel=next").when(repositoriesUrlConnection2).getHeaderField("Link");
        doReturn(repositoriesUrlConnection2).when(urlProvider).createUrlConnection("https://dummy.url/path?param=dummy&page=5");


        String apiUrl = "apiUrl";
        doReturn(installationsUrlConnection).when(urlProvider).createUrlConnection(eq(apiUrl + "/app/installations"));

        String appId = "appID";

        String apiPrivateKey;
        try (InputStream inputStream = getClass().getResourceAsStream("/rsa-private-key.pem")) {
            apiPrivateKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        RestApplicationAuthenticationProvider testCase = new RestApplicationAuthenticationProvider(clock, urlProvider);
        RepositoryAuthenticationToken result = testCase.getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath);

        assertEquals(expectedAuthenticationToken, result.getAuthenticationToken());
        assertEquals(expectedRepositoryId, result.getRepositoryId());

        ArgumentCaptor<String> requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(installationsUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(accessTokensUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(accessTokensUrlConnection).setRequestMethod("POST");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(repositoriesUrlConnection2, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(repositoriesUrlConnection2).setRequestMethod("GET");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer " + expectedAuthenticationToken),
                     requestPropertyArgumentCaptor.getAllValues());
    }

    @Test
    public void testExceptionOnNoMatchingToken() throws IOException {
        UrlConnectionProvider urlProvider = mock(UrlConnectionProvider.class);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(123456789L), ZoneId.of("UTC"));

        String expectedAuthenticationToken = "expected authentication token";
        String projectPath = "project path";
        String expectedRepositoryId = "expected repository Id";

        URLConnection installationsUrlConnection = mock(URLConnection.class);
        doReturn(new ByteArrayInputStream(
                "[{\"repositories_url\": \"repositories_url\", \"access_tokens_url\": \"tokens_url\"}]"
                        .getBytes(StandardCharsets.UTF_8))).when(installationsUrlConnection).getInputStream();

        HttpURLConnection accessTokensUrlConnection = mock(HttpURLConnection.class);
        doReturn(new ByteArrayInputStream(
                ("{\"token\": \"" + expectedAuthenticationToken + "\"}").getBytes(StandardCharsets.UTF_8)))
                .when(accessTokensUrlConnection).getInputStream();
        doReturn(accessTokensUrlConnection).when(urlProvider).createUrlConnection("tokens_url");


        HttpURLConnection repositoriesUrlConnection = mock(HttpURLConnection.class);
        doReturn(new ByteArrayInputStream(("{\"repositories\": [{\"node_id\": \"" + expectedRepositoryId +
                                           "\", \"full_name\": \"different_path\"}]}")
                                                  .getBytes(StandardCharsets.UTF_8))).when(repositoriesUrlConnection)
                .getInputStream();
        doReturn("").when(repositoriesUrlConnection).getHeaderField("Link");
        doReturn(repositoriesUrlConnection).when(urlProvider).createUrlConnection("repositories_url");

        String apiUrl = "apiUrl";
        doReturn(installationsUrlConnection).when(urlProvider).createUrlConnection(eq(apiUrl + "/app/installations"));

        String appId = "appID";

        String apiPrivateKey;
        try (InputStream inputStream = getClass().getResourceAsStream("/rsa-private-key.pem")) {
            apiPrivateKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        RestApplicationAuthenticationProvider testCase = new RestApplicationAuthenticationProvider(clock, urlProvider);
        assertThatThrownBy(() -> testCase.getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath)).hasMessage(
                "No token could be found with access to the requested repository with the given application ID and key")
                .isExactlyInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(installationsUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(accessTokensUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(accessTokensUrlConnection).setRequestMethod("POST");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(repositoriesUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(repositoriesUrlConnection).setRequestMethod("GET");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer " + expectedAuthenticationToken),
                     requestPropertyArgumentCaptor.getAllValues());

    }

    @Test
    public void testDefaultParameters() {
        Clock clock = mock(Clock.class);
        assertThat(new RestApplicationAuthenticationProvider(clock, new DefaultUrlConnectionProvider()))
                .usingRecursiveComparison().isEqualTo(new RestApplicationAuthenticationProvider(clock));
    }
}
