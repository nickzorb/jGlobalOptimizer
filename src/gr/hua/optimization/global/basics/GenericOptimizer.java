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
package gr.hua.optimization.global.basics;

import gr.hua.optimization.parameters.ParameterSet;
import gr.hua.optimization.parameters.ParameterSetIndex;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 *
 * @author Nikolaos Zormpas <nickzorb@gmail.com>
 */
public abstract class GenericOptimizer implements Optimizer {

    public class GenericResultStorage implements Optimizer.ResultStorage {

        Map<ParameterSetIndex, Comparable> storage = new HashMap();
        TreeMap<ParameterSetIndex, Comparable> sortedStorage = new TreeMap(new Comparator<ParameterSetIndex>() {

            @Override
            public int compare(ParameterSetIndex o1, ParameterSetIndex o2) {
                return storage.get(o1).compareTo(storage.get(o2)) == 1 ? 1 : -1;
            }
        });
        int limit;

        public GenericResultStorage(int limit) {
            this.limit = limit;
        }

        @Override
        public synchronized void result(ParameterSetIndex index, Comparable result) {
            storage.put(index, result);
        }

        @Override
        public Comparable result(ParameterSetIndex index) {
            return storage.get(index);
        }

        @Override
        public List<Entry<ParameterSetIndex, Comparable>> results() {
            List<Entry<ParameterSetIndex, Comparable>> res = new ArrayList();
            int i = 0;
            for (Entry<ParameterSetIndex, Comparable> e : sortedStorage.entrySet()) {
                if (i == limit) {
                    break;
                }
                res.add(e);
            }
            return res;
        }
    }

    @Override
    public synchronized List<Map.Entry<ParameterSetIndex, Comparable>> optimize(ParameterSet parameters, Evaluator<ParameterSetIndex, Comparable> function, int limit, int threads) {
        GenericResultStorage storage = new GenericResultStorage(limit);
        if (threads > 1) {
            return optimizeMulti(parameters, function, storage, threads);
        }
        return optimizeSingle(parameters, function, storage);
    }

    protected abstract List<Map.Entry<ParameterSetIndex, Comparable>> optimizeMulti(ParameterSet parameters, Evaluator<ParameterSetIndex, Comparable> function, ResultStorage storage, int threads);

    protected abstract List<Map.Entry<ParameterSetIndex, Comparable>> optimizeSingle(ParameterSet parameters, Evaluator<ParameterSetIndex, Comparable> function, ResultStorage storage);
}
