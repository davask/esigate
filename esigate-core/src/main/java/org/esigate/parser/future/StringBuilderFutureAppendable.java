/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.esigate.parser.future;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.esigate.HttpErrorPage;
import org.esigate.Parameters;

/**
 * This is an implementation of StringBuilder which can append Future&lt;CharSequence&gt; and is a
 * Future&lt;CharSequence&gt; itself.
 * <p>
 * It is intended for temporary buffers when implementing nested tags.
 * 
 * 
 * @author Nicolas Richeton
 * 
 */
public class StringBuilderFutureAppendable implements FutureAppendable, Future<CharSequence> {
    private final StringBuilder builder;
    private final FutureAppendableAdapter futureBuilder;

    /**
     * Create a new builder with a default capacity of 1024.
     */
    public StringBuilderFutureAppendable() {
        this(Parameters.DEFAULT_BUFFER_SIZE);
    }

    /**
     * Create a new builder with a custom capacity.
     * 
     * @param capacity
     *            Capacity of the builder.
     */
    private StringBuilderFutureAppendable(int capacity) {
        this.builder = new StringBuilder(capacity);
        this.futureBuilder = new FutureAppendableAdapter(this.builder);
    }

    @Override
    public FutureAppendable enqueueAppend(Future<CharSequence> csq) {
        return this.futureBuilder.enqueueAppend(csq);
    }

    @Override
    public FutureAppendable performAppends() throws IOException, HttpErrorPage {
        return this.futureBuilder.performAppends();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return this.futureBuilder.hasPending();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#get()
     */
    @Override
    public CharSequence get() throws ExecutionException {
        try {
            this.futureBuilder.performAppends();
        } catch (IOException | HttpErrorPage e) {
            throw new ExecutionException(e);
        }
        return this.builder.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public CharSequence get(long timeout, TimeUnit unit) throws ExecutionException {
        try {
            this.futureBuilder.performAppends();
        } catch (IOException | HttpErrorPage e) {
            throw new ExecutionException(e);
        }
        return this.builder.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.esigate.parser.future.FutureAppendable#hasPending()
     */
    @Override
    public boolean hasPending() {
        return this.futureBuilder.hasPending();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.esigate.parser.future.FutureAppendable#performAppends(int, java.util.concurrent.TimeUnit)
     */
    @Override
    public FutureAppendable performAppends(int timeout, TimeUnit unit) throws IOException, HttpErrorPage,
            TimeoutException {
        return this.futureBuilder.performAppends(timeout, unit);
    }

}
