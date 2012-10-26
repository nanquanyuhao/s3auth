/**
 * Copyright (c) 2012, s3auth.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the s3auth.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.s3auth.hosts;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Test case for {@link DefaultHost}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
public final class DefaultHostTest {

    /**
     * DefaultHost can load resource from S3.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void loadsAmazonResourcesFrom() throws Exception {
        final AmazonS3 aws = Mockito.mock(AmazonS3.class);
        Mockito.doAnswer(
            new Answer<S3Object>() {
                public S3Object answer(final InvocationOnMock invocation) {
                    final GetObjectRequest req = GetObjectRequest.class.cast(
                        invocation.getArguments()[0]
                    );
                    final S3Object object = new S3Object();
                    object.setObjectContent(
                        IOUtils.toInputStream(req.getKey())
                    );
                    return object;
                }
            }
        ).when(aws).getObject(Mockito.any(GetObjectRequest.class));
        final Host host = new DefaultHost(
            new BucketMocker().withClient(aws).mock()
        );
        final Map<String, String> paths = new HashMap<String, String>() {
            private static final long serialVersionUID = 0x75294A7898F21489L;
            {
                this.put("/index.html?q", "index.htm");
                this.put("/", "index.htm");
                this.put("/foo/index.html", "foo/index.html");
                this.put("/foo/", "foo/index.htm");
            }
        };
        for (Map.Entry<String, String> path : paths.entrySet()) {
            MatcherAssert.assertThat(
                ResourceMocker.toString(host.fetch(URI.create(path.getKey()))),
                Matchers.equalTo(path.getValue())
            );
        }
    }

    /**
     * DefaultHost can throw IOException for absent object.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = java.io.IOException.class)
    public void throwsWhenAbsentResource() throws Exception {
        final Host host = new DefaultHost(
            new DefaultBucket(
                new DomainMocker()
                    .withName("invalid-bucket.s3auth.com")
                    .withKey("foo")
                    .withSecret("invalid-data")
                    .mock()
            )
        );
        host.fetch(URI.create("foo.html"));
    }

    /**
     * DefaultHost can show some stats in {@code #toString()}.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void showsStatsInToString() throws Exception {
        MatcherAssert.assertThat(
            new DefaultHost(new BucketMocker().mock()),
            Matchers.hasToString(Matchers.notNullValue())
        );
    }

}
