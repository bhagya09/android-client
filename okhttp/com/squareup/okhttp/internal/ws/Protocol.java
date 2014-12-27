/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.okhttp.internal.ws;

final class Protocol {
  /*
  Each frame starts with two bytes of data.

   0 1 2 3 4 5 6 7    0 1 2 3 4 5 6 7
  +-+-+-+-+-------+  +-+-------------+
  |F|R|R|R| OP    |  |M| LENGTH      |
  |I|S|S|S| CODE  |  |A|             |
  |N|V|V|V|       |  |S|             |
  | |1|2|3|       |  |K|             |
  +-+-+-+-+-------+  +-+-------------+
  */
	/** Byte 0 flag for whether this is the final fragment in a message. */
	static final int B0_FLAG_FIN = Integer.parseInt("10000000",2);
	/** Byte 0 reserved flag 1. Must be 0 unless negotiated otherwise. */
	static final int B0_FLAG_RSV1 = Integer.parseInt("01000000",2);
	/** Byte 0 reserved flag 2. Must be 0 unless negotiated otherwise. */
	static final int B0_FLAG_RSV2 = Integer.parseInt("00100000",2);
	/** Byte 0 reserved flag 3. Must be 0 unless negotiated otherwise. */
	static final int B0_FLAG_RSV3 = Integer.parseInt("00010000",2);
	/** Byte 0 mask for the frame opcode. */
	static final int B0_MASK_OPCODE = Integer.parseInt("00001111",2);
	/** Flag in the opcode which indicates a control frame. */
	static final int OPCODE_FLAG_CONTROL = Integer.parseInt("00001000",2);

//  /** Byte 0 flag for whether this is the final fragment in a message. */
//  static final int B0_FLAG_FIN = 0b10000000;
//  /** Byte 0 reserved flag 1. Must be 0 unless negotiated otherwise. */
//  static final int B0_FLAG_RSV1 = 0b01000000;
//  /** Byte 0 reserved flag 2. Must be 0 unless negotiated otherwise. */
//  static final int B0_FLAG_RSV2 = 0b00100000;
//  /** Byte 0 reserved flag 3. Must be 0 unless negotiated otherwise. */
//  static final int B0_FLAG_RSV3 = 0b00010000;
//  /** Byte 0 mask for the frame opcode. */
//  static final int B0_MASK_OPCODE = 0b00001111;
//  /** Flag in the opcode which indicates a control frame. */
//  static final int OPCODE_FLAG_CONTROL = 0b00001000;

  /**
   * Byte 1 flag for whether the payload data is masked.
   * <p>
   * If this flag is set, the next four bytes represent the mask key. These bytes appear after
   * any additional bytes specified by {@link #B1_MASK_LENGTH}.
   */
  //static final int B1_FLAG_MASK = 0b10000000;
  static final int B1_FLAG_MASK = Integer.parseInt("10000000",2);
  /**
   * Byte 1 mask for the payload length.
   * <p>
   * If this value is {@link #PAYLOAD_SHORT}, the next two bytes represent the length.
   * If this value is {@link #PAYLOAD_LONG}, the next eight bytes represent the length.
   */
  //static final int B1_MASK_LENGTH = 0b01111111;
  static final int B1_MASK_LENGTH = Integer.parseInt("01111111",2);

  static final int OPCODE_CONTINUATION = 0x0;
  static final int OPCODE_TEXT = 0x1;
  static final int OPCODE_BINARY = 0x2;

  static final int OPCODE_CONTROL_CLOSE = 0x8;
  static final int OPCODE_CONTROL_PING = 0x9;
  static final int OPCODE_CONTROL_PONG = 0xa;

  /**
   * Maximum length of frame payload. Larger payloads, if supported, can use the special values
   * {@link #PAYLOAD_SHORT} or {@link #PAYLOAD_LONG}.
   */
  static final int PAYLOAD_MAX = 125;
  /** Value for {@link #B1_MASK_LENGTH} which indicates the next two bytes are the length. */
  static final int PAYLOAD_SHORT = 126;
  /** Value for {@link #B1_MASK_LENGTH} which indicates the next eight bytes are the length. */
  static final int PAYLOAD_LONG = 127;

  static void toggleMask(byte[] buffer, long byteCount, byte[] key, long frameBytesRead) {
    int keyLength = key.length;
    for (int i = 0; i < byteCount; i++, frameBytesRead++) {
      int keyIndex = (int) (frameBytesRead % keyLength);
      buffer[i] = (byte) (buffer[i] ^ key[keyIndex]);
    }
  }

  private Protocol() {
    throw new AssertionError("No instances.");
  }
}
