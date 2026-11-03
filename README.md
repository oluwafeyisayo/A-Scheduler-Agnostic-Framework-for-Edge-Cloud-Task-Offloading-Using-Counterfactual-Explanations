# A Scheduler-Agnostic Counterfactual Framework for Edge-Cloud Task Offloading Using Counterfactual Explanations

## Overview

This repository contains the implementation and evaluation of a scheduler-agnostic counterfactual framework for edge-cloud task offloading. The framework generates feasible counterfactual explanations for scheduling decisions by reconstructing historical scheduler states, modifying bounded environmental conditions, and replaying decisions to determine what changes could have produced an alternative scheduling outcome.

The framework is designed to operate across heterogeneous scheduling policies without requiring scheduler-specific counterfactual generation logic.

## Framework

The proposed framework consists of four main stages:

1. Decision-State Capture and Provenance Construction
   Captures scheduling decisions, system conditions, resource states, and candidate evidence required to reconstruct historical scheduling states.

2. Replay-Guided Counterfactual Search
   Performs adaptive search over mutable system and network features to identify candidate interventions capable of changing the original scheduling outcome.

3. Feasibility-Constrained Scheduler Replay
   Reconstructs modified decision states and re-executes the underlying scheduler while enforcing resource, network, and execution feasibility constraints.

4. Counterfactual Refinement and Ranking
   Applies neighborhood refinement and ranking to verified candidates to select feasible and higher-quality counterfactual explanations.

## Supported Schedulers

The framework is evaluated across five heterogeneous scheduling policies:

* RANDOM
* ROUND_ROBIN
* LEAST_LOADED
* MULTI_OBJECTIVE
* PPO

These schedulers represent stochastic, cyclic, resource-aware, optimization-based, and reinforcement-learning-based scheduling strategies.

## Counterfactual Objectives

The framework supports three counterfactual objectives:

* **Execution-Target Change:** Identify conditions under which a task would execute on a different computing node.
* **Destination Change:** Identify conditions that would change the selected offloading destination.
* **Feasibility Restoration:** Identify changes capable of converting an originally infeasible scheduling decision into a feasible one.

## Evaluation

The experimental evaluation examines:

* Counterfactual generation effectiveness
* Replay fidelity
* Scheduler generalizability
* Search and replay efficiency
* Scalability across different numbers of edge devices
* Sensitivity to network constraints
* Effect of adaptive versus exhaustive replay-guided search
* Effect of neighborhood refinement
* Counterfactual quality

Experiments are implemented using **EdgeCloudSim** under heterogeneous workload, network, and scheduling conditions.

## Repository Structure

The codebase contains modules responsible for:

* Decision and provenance logging
* Scheduler implementations
* Historical state reconstruction
* Deterministic scheduler replay
* Adaptive replay-guided feasible counterfactual search
* Feasibility validation
* Neighborhood refinement
* Counterfactual ranking
* Experiment configuration and execution
* Metric aggregation and result analysis

## Purpose

The framework provides an explainability layer for edge-cloud task offloading systems by answering questions such as:

> **What minimal feasible changes to the observed system conditions could have resulted in a different scheduling decision?**

By separating counterfactual generation from the internal structure of individual schedulers, the approach enables a common explanation mechanism to be applied across heterogeneous edge-cloud scheduling policies.

## Paper

** A Scheduler-Agnostic Counterfactual Framework for Edge-Cloud Task Offloading Using Counterfactual Explanations**
