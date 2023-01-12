@description('Name of the dashboard to display in Azure portal')
param dashboardDisplayName string = 'EventHubs for Java SDK dashboard'

@description('ApplicationInsights resource id with EventHubs SDK metrics.')
param applicationInsightResourceId string

@metadata({ Description: 'Resource name that Azure portal uses for the dashboard' })
param dashboardName string = guid(applicationInsightResourceId, dashboardDisplayName)
param location string = resourceGroup().location

resource dashboard 'Microsoft.Portal/dashboards@2020-09-01-preview' = {
  name: dashboardName
  location: location
  tags: {
    'hidden-title': dashboardDisplayName
  }
  properties: {
    lenses: [
      {
        order: 0
        parts: [
          {
            position: {
              x: 0
              y: 0
              colSpan: 8
              rowSpan: 4
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Sent and received message rate'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.eventhubs.consumer.lag'
                          aggregationType: 7
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'received'
                          }                          
                        }
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.eventhubs.events.sent'
                          aggregationType: 1
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'sent'
                          }                          
                        }
                      ]
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 8
              y: 0
              colSpan: 8
              rowSpan: 4
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Consumer lag per partition, seconds'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.eventhubs.consumer.lag'
                          aggregationType: 4
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'lag'
                          }                          
                        }
                      ]
                      grouping: {
                        dimension: 'messaging.eventhubs.partition_id'
                        sort: 2
                        top: 50
                      }                      
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 16
              y: 0
              colSpan: 3
              rowSpan: 3
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Send error rate'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.az.amqp.producer.send.duration'
                          aggregationType: 7
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'errors'
                          }                          
                        }
                      ]
                      filterCollection: {
                        filters: [
                          {
                            key: 'amqp.delivery_state'
                            operator: 1
                            values: [
                              'accepted'
                            ]
                          }
                        ]
                      }
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 19
              y: 0
              colSpan: 3
              rowSpan: 3
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Management request error rate by operation'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.az.amqp.management.request.duration'
                          aggregationType: 7
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'management operations'
                          }                          
                        }
                      ]
                      grouping: {
                        dimension: 'amqp.operation'
                        sort: 2
                        top: 10
                      }
                      filterCollection: {
                        filters: [
                          {
                            key: 'amqp.status_code'
                            operator: 1
                            values: [
                              'accepted'
                            ]
                          }
                        ]
                      }
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 16
              y: 3
              colSpan: 3
              rowSpan: 3
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Closed connections rate'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.az.amqp.client.connections.closed'
                          aggregationType: 7
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'closed connections'
                          }                          
                        }
                      ]
                      grouping: {
                        dimension: 'amqp.error_condition'
                        sort: 2
                        top: 10
                      }      
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 19
              y: 3
              colSpan: 3
              rowSpan: 3
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'AMQP error rate'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.az.amqp.client.link.errors'
                          aggregationType: 7
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'link errors'
                          }                          
                        }
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.az.amqp.client.session.errors'
                          aggregationType: 7
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'session errors'
                          }                          
                        }
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.az.amqp.client.transport.errors'
                          aggregationType: 7
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'transport errors'
                          }                          
                        }
                      ]
                      grouping: {
                        dimension: 'amqp.error_condition'
                        sort: 2
                        top: 10
                      }
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 0
              y: 4
              colSpan: 8
              rowSpan: 4
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Send, management, and checkpoint request duration (average), ms'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.eventhubs.checkpoint.duration'
                          aggregationType: 4
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'checkpoint duration'
                          }                          
                        }
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.az.amqp.management.request.duration'
                          aggregationType: 4
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'management'
                          }                          
                        }
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.az.amqp.producer.send.duration'
                          aggregationType: 4
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'send'
                          }                          
                        }                        
                      ]
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 16
              y: 9
              colSpan: 6
              rowSpan: 3
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Last checkpointed sequence number per partition'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.eventhubs.checkpoint.sequence_number'
                          aggregationType: 3
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'sequence number'
                          }                          
                        }
                      ]
                      grouping: {
                        dimension: 'messaging.eventhubs.partition_id'
                        sort: 2
                        top: 10
                      }
                      visualization: {
                        chartType: 5
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 8
              y: 4
              colSpan: 8
              rowSpan: 4
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Credit request rate'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.az.amqp.consumer.credits.requested'
                          aggregationType: 3
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'credits'
                          }                          
                        }
                      ]
                      grouping: {
                        dimension: 'messaging.az.entity_path'
                        sort: 2
                        top: 50
                      }    
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 0
              y: 8
              colSpan: 8
              rowSpan: 4
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Checkpoint rate per partition'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.eventhubs.checkpoint.duration'
                          aggregationType: 7
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'checkpoints'
                          }                          
                        }
                      ]
                      grouping: {
                        dimension: 'messaging.eventhubs.partition_id'
                        sort: 2
                        top: 10
                      }                      
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 8
              y: 8
              colSpan: 8
              rowSpan: 4
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Received message rate per partition'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.eventhubs.consumer.lag'
                          aggregationType: 7
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'received events'
                          }                          
                        }                  
                      ]
                      grouping: {
                        dimension: 'messaging.eventhubs.partition_id'
                        sort: 2
                        top: 10
                      }                      
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
          {
            position: {
              x: 16
              y: 6
              colSpan: 6
              rowSpan: 3
            }
            metadata: {
              inputs: [
                {
                  name: 'options'
                  value: {
                    chart: {
                      title: 'Checkpoint error rate per partition'
                      metrics: [
                        {
                          resourceMetadata: {
                            id: applicationInsightResourceId
                          }
                          name: 'messaging.eventhubs.checkpoint.duration'
                          aggregationType: 7
                          namespace: 'azure.applicationinsights'
                          metricVisualization: {
                            displayName: 'checkpoints'
                          }                          
                        }                  
                      ]
                      grouping: {
                        dimension: 'messaging.eventhubs.partition_id'
                        sort: 2
                        top: 50
                      }
                      filterCollection: {
                        filters: [
                          {
                            key: 'otel.status_code'
                            operator: 1
                            values: [
                              'ok'
                            ]
                          }
                        ]
                      }                                      
                      visualization: {
                        chartType: 2
                        legendVisualization: {
                          isVisible: true
                          position: 2
                          hideSubtitle: false
                        }
                        axisVisualization: {
                          x: {
                            isVisible: true
                            axisType: 2
                          }
                          y: {
                            isVisible: true
                            axisType: 1
                          }
                        }
                      }
                    }
                  }
                }
                {
                  name: 'sharedTimeRange'
                  isOptional: true
                }
              ]
              #disable-next-line BCP036
              type: 'Extension/HubsExtension/PartType/MonitorChartPart'
              settings: {}
            }
          }
        ]
      }
    ]
    metadata: {
      model: {
        timeRange: {
          value: {
            relative: {
              duration: 4
              timeUnit: 1
            }
          }
          type: 'MsPortalFx.Composition.Configuration.ValueTypes.TimeRange'
        }
        filterLocale: {
          value: 'en-us'
        }
        filters: {
          value: {
            MsPortalFx_TimeRange: {
              model: {
                format: 'local'
                granularity: 'auto'
                relative: '4h'
              }
              displayCache: {
                name: 'Local Time'
                value: 'Past 4 hours'
              }
              filteredPartIds: []              
            }
            'net.peer.name': {
              model: {
                operator: 'equals'
                selectAllState: 'all'
              }
              displayCache: {
                name: 'net.peer.name'
                value: 'all'
              }
              filteredPartIds: []              
            }
            'messaging.destination': {
              model: {
                operator: 'equals'
                selectAllState: 'all'
              }
              displayCache: {
                name: 'messaging.destination'
                value: 'all'
              }
              filteredPartIds: []
            }
          }
        }
      }
    }
  }
}
