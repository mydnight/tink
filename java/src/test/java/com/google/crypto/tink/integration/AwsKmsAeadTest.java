// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.crypto.tink.integration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.AWSKMSException;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.subtle.Random;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for AwsKmsAead.
 */
@RunWith(MockitoJUnitRunner.class)
public class AwsKmsAeadTest {

  @Mock private AWSKMS mockKms;
  private final String keyId = "aws-kms://123";

  @Test
  public void testEncryptDecrypt() throws Exception {
    DecryptResult mockDecryptResult = mock(DecryptResult.class);
    EncryptResult mockEncryptResult = mock(EncryptResult.class);
    when(mockKms.decrypt(isA(DecryptRequest.class)))
        .thenReturn(mockDecryptResult);
    when(mockKms.encrypt(isA(EncryptRequest.class)))
        .thenReturn(mockEncryptResult);

    Aead aead = new AwsKmsAead(mockKms, keyId);
    byte[] aad = Random.randBytes(20);
    for (int messageSize = 0; messageSize < 75; messageSize++) {
      byte[] message = Random.randBytes(messageSize);
      when(mockDecryptResult.getPlaintext()).thenReturn(ByteBuffer.wrap(message));
      when(mockEncryptResult.getCiphertextBlob()).thenReturn(ByteBuffer.wrap(message));
      byte[] ciphertext = aead.encrypt(message, aad);
      byte[] decrypted = aead.decrypt(ciphertext, aad);
      assertArrayEquals(message, decrypted);
    }
  }

  @Test
  public void testEncrypt_shouldThrowExceptionIfRequestFailed() throws Exception {
    AWSKMSException exception = mock(AWSKMSException.class);
    when(mockKms.encrypt(isA(EncryptRequest.class)))
        .thenThrow(exception);

    Aead aead = new AwsKmsAead(mockKms, keyId);
    byte[] aad = Random.randBytes(20);
    byte[] message = Random.randBytes(20);
    try {
      aead.encrypt(message, aad);
      fail("Expected GeneralSecurityException");
    } catch (GeneralSecurityException e) {
      // expected.
    }
  }

  @Test
  public void testDecrypt_shouldThrowExceptionIfRequestFailed() throws Exception {
    EncryptResult mockEncryptResult = mock(EncryptResult.class);
    when(mockKms.encrypt(isA(EncryptRequest.class)))
        .thenReturn(mockEncryptResult);
    AWSKMSException exception = mock(AWSKMSException.class);
    when(mockKms.decrypt(isA(DecryptRequest.class)))
        .thenThrow(exception);

    Aead aead = new AwsKmsAead(mockKms, keyId);
    byte[] aad = Random.randBytes(20);
    byte[] message = Random.randBytes(20);
    when(mockEncryptResult.getCiphertextBlob()).thenReturn(ByteBuffer.wrap(message));
    byte[] ciphertext = aead.encrypt(message, aad);
    try {
      aead.decrypt(ciphertext, aad);
      fail("Expected GeneralSecurityException");
    } catch (GeneralSecurityException e) {
      // expected.
    }
  }
}