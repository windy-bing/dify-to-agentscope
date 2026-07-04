app:
  description: Generic demo workflow without business-specific semantics.
  icon: 🤖
  icon_background: '#FFEAD5'
  icon_type: emoji
  mode: advanced-chat
  name: AgentScope Generic Demo
  use_icon_as_answer_icon: false
dependencies: []
kind: app
version: 0.6.0
workflow:
  conversation_variables:
    - description: Demo conversation round.
      id: var-chat-round
      name: chat_round
      selector:
        - conversation
        - chat_round
      value: 0
      value_type: integer
    - description: Latest generic item identifier.
      id: var-demo-item-id
      name: demo_item_id
      selector:
        - conversation
        - demo_item_id
      value: ''
      value_type: string
    - description: Latest generic request identifier.
      id: var-demo-request-id
      name: demo_request_id
      selector:
        - conversation
        - demo_request_id
      value: ''
      value_type: string
    - description: Latest generic task identifier.
      id: var-demo-task-id
      name: demo_task_id
      selector:
        - conversation
        - demo_task_id
      value: ''
      value_type: string
    - description: Demo reply cache.
      id: var-reply
      name: reply
      selector:
        - conversation
        - reply
      value: ''
      value_type: string
    - description: Historical demo queries.
      id: var-history-user-query
      name: history_user_query
      selector:
        - conversation
        - history_user_query
      value: []
      value_type: array[object]
  environment_variables: []
  features:
    opening_statement: Generic AgentScope demo workflow. Try ITEM-1001, REQ-2001, or TASK-3001.
  graph:
    nodes:
      - id: start
        data:
          title: Start
          type: start
        type: custom
      - id: extract-demo-identifiers
        data:
          title: Extract Demo Identifiers
          type: code
          outputs:
            demo_item_id:
              type: string
            demo_item_id_list:
              type: array[string]
            demo_request_id:
              type: string
            demo_task_id:
              type: string
            history_chat:
              type: string
        type: custom
      - id: route-demo
        data:
          title: Route Demo Identifier
          type: if-else
          cases:
            - case_id: item
              logical_operator: and
              conditions:
                - variable_selector:
                    - extract-demo-identifiers
                    - demo_item_id
                  comparison_operator: not empty
                  value: ''
            - case_id: request
              logical_operator: and
              conditions:
                - variable_selector:
                    - extract-demo-identifiers
                    - demo_request_id
                  comparison_operator: not empty
                  value: ''
            - case_id: task
              logical_operator: and
              conditions:
                - variable_selector:
                    - extract-demo-identifiers
                    - demo_task_id
                  comparison_operator: not empty
                  value: ''
        type: custom
      - id: demo-item-agent
        data:
          title: DemoItemAgent
          type: agent
          agent_parameters:
            instruction:
              value: Reply as a generic demo item agent.
            model:
              value:
                model: dashscope:qwen-plus
                completion_params: {}
            tools:
              value: []
        type: custom
      - id: demo-request-agent
        data:
          title: DemoRequestAgent
          type: agent
          agent_parameters:
            instruction:
              value: Reply as a generic demo request agent.
            model:
              value:
                model: dashscope:qwen-plus
                completion_params: {}
            tools:
              value: []
        type: custom
      - id: demo-task-agent
        data:
          title: DemoTaskAgent
          type: agent
          agent_parameters:
            instruction:
              value: Reply as a generic demo task agent.
            model:
              value:
                model: dashscope:qwen-plus
                completion_params: {}
            tools:
              value: []
        type: custom
      - id: demo-fallback-agent
        data:
          title: DemoFallbackAgent
          type: agent
          agent_parameters:
            instruction:
              value: Reply as a generic fallback demo agent.
            model:
              value:
                model: dashscope:qwen-plus
                completion_params: {}
            tools:
              value: []
        type: custom
      - id: set-item-reply
        data:
          title: Set Item Reply
          type: assigner
          items:
            - variable_selector:
                - conversation
                - demo_item_id
              input_type: variable
              value:
                - extract-demo-identifiers
                - demo_item_id
              operation: set
            - variable_selector:
                - conversation
                - reply
              input_type: variable
              value:
                - demo-item-agent
                - text
              operation: set
        type: custom
      - id: set-request-reply
        data:
          title: Set Request Reply
          type: assigner
          items:
            - variable_selector:
                - conversation
                - demo_request_id
              input_type: variable
              value:
                - extract-demo-identifiers
                - demo_request_id
              operation: set
            - variable_selector:
                - conversation
                - reply
              input_type: variable
              value:
                - demo-request-agent
                - text
              operation: set
        type: custom
      - id: set-task-reply
        data:
          title: Set Task Reply
          type: assigner
          items:
            - variable_selector:
                - conversation
                - demo_task_id
              input_type: variable
              value:
                - extract-demo-identifiers
                - demo_task_id
              operation: set
            - variable_selector:
                - conversation
                - reply
              input_type: variable
              value:
                - demo-task-agent
                - text
              operation: set
        type: custom
      - id: set-fallback-reply
        data:
          title: Set Fallback Reply
          type: assigner
          items:
            - variable_selector:
                - conversation
                - reply
              input_type: variable
              value:
                - demo-fallback-agent
                - text
              operation: set
        type: custom
      - id: answer
        data:
          title: Answer
          type: answer
          answer: '{{#conversation.reply#}}'
        type: custom
    edges:
      - id: start-source-extract-demo-identifiers
        source: start
        sourceHandle: source
        target: extract-demo-identifiers
        targetHandle: target
        type: custom
      - id: extract-demo-identifiers-source-route-demo
        source: extract-demo-identifiers
        sourceHandle: source
        target: route-demo
        targetHandle: target
        type: custom
      - id: route-demo-item-demo-item-agent
        source: route-demo
        sourceHandle: item
        target: demo-item-agent
        targetHandle: target
        type: custom
      - id: route-demo-request-demo-request-agent
        source: route-demo
        sourceHandle: request
        target: demo-request-agent
        targetHandle: target
        type: custom
      - id: route-demo-task-demo-task-agent
        source: route-demo
        sourceHandle: task
        target: demo-task-agent
        targetHandle: target
        type: custom
      - id: route-demo-false-demo-fallback-agent
        source: route-demo
        sourceHandle: 'false'
        target: demo-fallback-agent
        targetHandle: target
        type: custom
      - id: demo-item-agent-source-set-item-reply
        source: demo-item-agent
        sourceHandle: source
        target: set-item-reply
        targetHandle: target
        type: custom
      - id: demo-request-agent-source-set-request-reply
        source: demo-request-agent
        sourceHandle: source
        target: set-request-reply
        targetHandle: target
        type: custom
      - id: demo-task-agent-source-set-task-reply
        source: demo-task-agent
        sourceHandle: source
        target: set-task-reply
        targetHandle: target
        type: custom
      - id: demo-fallback-agent-source-set-fallback-reply
        source: demo-fallback-agent
        sourceHandle: source
        target: set-fallback-reply
        targetHandle: target
        type: custom
      - id: set-item-reply-source-answer
        source: set-item-reply
        sourceHandle: source
        target: answer
        targetHandle: target
        type: custom
      - id: set-request-reply-source-answer
        source: set-request-reply
        sourceHandle: source
        target: answer
        targetHandle: target
        type: custom
      - id: set-task-reply-source-answer
        source: set-task-reply
        sourceHandle: source
        target: answer
        targetHandle: target
        type: custom
      - id: set-fallback-reply-source-answer
        source: set-fallback-reply
        sourceHandle: source
        target: answer
        targetHandle: target
        type: custom
