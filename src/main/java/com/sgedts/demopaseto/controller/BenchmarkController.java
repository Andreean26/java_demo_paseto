package com.sgedts.demopaseto.controller;

import com.sgedts.demopaseto.service.BenchmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> runGet(
            @RequestParam(defaultValue = "1000") int iterations,
            @RequestParam(defaultValue = "10") int rounds,
            @RequestParam(defaultValue = "standard") String preset) throws Exception {

        Map<String, Object> options = Map.of(
                "iterations", iterations,
                "rounds", rounds,
                "preset", preset
        );
        return ResponseEntity.ok(benchmarkService.runBenchmark(options));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> runPost(
            @RequestBody(required = false) Map<String, Object> body) throws Exception {
        return ResponseEntity.ok(benchmarkService.runBenchmark(body != null ? body : Map.of()));
    }
}
