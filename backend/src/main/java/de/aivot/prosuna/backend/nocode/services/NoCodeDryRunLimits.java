package de.aivot.prosuna.backend.nocode.services;

import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;
import java.util.ArrayDeque;

/**
 * Limits only editor tests, never stored definitions or normal process execution.
 */
final class NoCodeDryRunLimits {
    static final int MAX_NODES = 512;
    static final int MAX_DEPTH = 32;
    static final int MAX_REQUEST_BYTES = 256 * 1024;
    static final int MAX_RESULT_BYTES = 1024 * 1024;

    private record Entry(@Nullable NoCodeOperand operand, int depth) {
    }

    static boolean expressionFits(@Nullable NoCodeOperand operand) {
        var pending = new ArrayDeque<Entry>();
        pending.push(new Entry(operand, 1));
        int count = 0;
        while (!pending.isEmpty()) {
            var entry = pending.pop();
            if (++count > MAX_NODES || entry.depth() > MAX_DEPTH) return false;
            if (entry.operand() instanceof NoCodeExpression expression && expression.getOperands() != null) {
                if (expression.getOperands().size() > MAX_NODES - count) return false;
                for (var child : expression.getOperands()) pending.push(new Entry(child, entry.depth() + 1));
            }
        }
        return true;
    }

    /**
     * Count encoded bytes without retaining a second copy of large results in memory.
     */
    static boolean jsonFits(@Nonnull JsonMapper mapper, @Nullable Object value, int maximumBytes) {
        return jsonSizeWithinLimit(mapper, value, maximumBytes) >= 0;
    }

    static int jsonSizeWithinLimit(@Nonnull JsonMapper mapper, @Nullable Object value, int maximumBytes) {
        var output = new CountingOutput(maximumBytes);
        try {
            mapper.writeValue(output, value);
            return output.count;
        } catch (RuntimeException exception) {
            if (output.exceeded) return -1;
            throw exception;
        }
    }

    private static final class CountingOutput extends OutputStream {
        private final int maximum;
        private int count;
        private boolean exceeded;

        private CountingOutput(int maximum) {
            this.maximum = maximum;
        }

        @Override
        public void write(int value) {
            add(1);
        }

        @Override
        public void write(@Nonnull byte[] bytes, int offset, int length) {
            add(length);
        }

        private void add(int length) {
            if (length > maximum - count) {
                exceeded = true;
                throw new IllegalStateException("Dry-run JSON size limit exceeded");
            }
            count += length;
        }
    }
}
