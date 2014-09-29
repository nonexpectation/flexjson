/**
 * Copyright 2007 Charlie Hubbard and Brandon Goodin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package flexjson;

public class StringBufferOutputHandler implements OutputHandler {

    private StringBuffer out;

    public StringBufferOutputHandler(StringBuffer out) {
        this.out = out;
    }

    public OutputHandler write(String value) {
        out.append(value);
        return this;
    }

    public OutputHandler write(char c) {
        out.append(c);
        return this;
    }

    public int write(String value, int start, int end, String append) {
        out.append( value, start, end );
        out.append( append );
        return end + 1;
    }

    public int write(String value, int start, int end) {
        out.append( value, start, end );
        return end;
    }

    public String toString() {
        return out.toString();
    }
}