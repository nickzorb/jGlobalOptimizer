/*
 * Copyright 2015 Nikolaos Zormpas <nickzorb@gmail.com>.
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
package gr.hua.optimization.global.brute;

import gr.hua.optimization.global.basics.Evaluator;
import gr.hua.optimization.global.basics.GenericOptimizer;
import gr.hua.optimization.parameters.ParameterSet;
import gr.hua.optimization.parameters.ParameterSetIndex;
import gr.hua.utils.range.RangeIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Nikolaos Zormpas <nickzorb@gmail.com>
 */
public class BruteForceOptimizer extends GenericOptimizer {

    @Override
    protected List<Map.Entry<ParameterSetIndex, Comparable>> optimizeMulti(ParameterSet parameters, Evaluator<ParameterSetIndex, Comparable> function, ResultStorage storage, int threads) {
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        ParameterSetIndex psi = parameters.index();
        List<RangeIterator> iterators = new ArrayList(psi.iterators().values());
        loopIterators(iterators, 0, ex, 0, threads, storage, function, psi);
        ex.shutdown();
        try {
            ex.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return storage.results();
    }

    protected void loopIterators(List<RangeIterator> iterators, int level, ExecutorService ex, int running, int totalThreads, ResultStorage storage, Evaluator<ParameterSetIndex, Comparable> function, ParameterSetIndex psi) {
        if (ex != null) {
            while (iterators.get(level).hasNext()) {
                ex.submit(() -> {
                    iterators.get(level).next();
                    if (iterators.size() > level + 1) {
                        if (running >= 4 * totalThreads) {
                            loopIterators(iterators, level + 1, null, 0, 0, storage, function, psi);
                        } else {
                            loopIterators(iterators, level + 1, ex, running, totalThreads, storage, function, psi);
                        }
                    } else {
                        storage.result(psi.clone(), function.accept(psi));
                    }
                });
            }
            return;
        }
        while (iterators.get(level).hasNext()) {
            iterators.get(level).next();
            if (iterators.size() > level + 1) {
                loopIterators(iterators, level + 1, ex, running, totalThreads, storage, function, psi);
            } else {
                storage.result(psi.clone(), function.accept(psi));
            }
        }
    }

    @Override
    protected List<Map.Entry<ParameterSetIndex, Comparable>> optimizeSingle(ParameterSet parameters, Evaluator<ParameterSetIndex, Comparable> function, ResultStorage storage) {
        ParameterSetIndex psi = parameters.index();
        List<RangeIterator> iterators = new ArrayList(psi.iterators().values());
        loopIterators(iterators, 0, null, 0, 0, storage, function, psi);
        return storage.results();
    }
}
