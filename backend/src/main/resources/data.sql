INSERT INTO agents(name, active_order_count, status) VALUES
                                                         ('Priya Sharma', 2, 'BUSY'),
                                                         ('Rahul Verma', 0, 'AVAILABLE'),
                                                         ('Ananya Iyer', 1, 'BUSY'),
                                                         ('Kiran Nair', 0, 'AVAILABLE'),
                                                         ('Deepak Mehta', 3, 'BUSY');

INSERT INTO orders(description, assigned_agent_id, status, pickup_zone, dropoff_zone, weight_class) VALUES
                                                                                                        ('Electronics - Koramangala to Indiranagar', 1, 'ASSIGNED', 'Koramangala', 'Indiranagar', 'LIGHT'),
                                                                                                        ('Groceries - HSR Layout to BTM', 1, 'ASSIGNED', 'HSR Layout', 'BTM', 'LIGHT'),
                                                                                                        ('Pharma - Whitefield to Marathahalli', 3, 'ASSIGNED', 'Whitefield', 'Marathahalli', 'LIGHT'),
                                                                                                        ('Documents - MG Road to Jayanagar', 5, 'ASSIGNED', 'MG Road', 'Jayanagar', 'LIGHT'),
                                                                                                        ('Food - Bellandur to Electronic City', 5, 'ASSIGNED', 'Bellandur', 'Electronic City', 'LIGHT'),
                                                                                                        ('Apparel - Malleshwaram to Rajajinagar', 5, 'ASSIGNED', 'Malleshwaram', 'Rajajinagar', 'LIGHT'),
                                                                                                        ('Books - Banashankari to JP Nagar', 3, 'ASSIGNED', 'Banashankari', 'JP Nagar', 'LIGHT'),
                                                                                                        ('Hardware - Peenya to Yeshwanthpur', 1, 'ASSIGNED', 'Peenya', 'Yeshwanthpur', 'HEAVY');